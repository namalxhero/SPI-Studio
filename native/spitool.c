/* spitool: encode/decode Samsung .spi (MAET codec) using the device's own libmaet.so
 *   spitool dec <in.spi> <out.rgba>            prints "W H"
 *   spitool enc <in.rgba> <w> <h> <out.spi>    lossless, verifies by decoding (prints EXACT)
 * env MAET_LIB overrides /system/lib64/libmaet.so */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define BUFSZ (24u << 20)
typedef int (*f_init)(void);
typedef void *(*f_create)(void *, void *);
typedef int (*f_out)(void *, void *, void *);
typedef int (*f_push)(void *, void *);
typedef int (*f_clone)(void *, void *);
typedef int (*f_cfg)(void *, int, void *, void *);

static struct {
    f_init init; f_create ecreate, dcreate; f_out ehdr, eenc, ddec;
    f_push epush; f_clone dclone; f_cfg dcfg;
} L;

static void put32(uint8_t *b, size_t o, uint32_t v) { memcpy(b + o, &v, 4); }
static void put64(uint8_t *b, size_t o, uint64_t v) { memcpy(b + o, &v, 8); }
static int32_t get32(const uint8_t *b, size_t o) { int32_t v; memcpy(&v, b + o, 4); return v; }

static int load(void) {
    const char *p = getenv("MAET_LIB");
    void *h = dlopen(p ? p : "/system/lib64/libmaet.so", RTLD_NOW);
    if (!h) { fprintf(stderr, "dlopen failed: %s\n", dlerror()); return 0; }
    L.init = (f_init)dlsym(h, "maet_init");
    L.ecreate = (f_create)dlsym(h, "maete_create");
    L.dcreate = (f_create)dlsym(h, "maetd_create");
    L.ehdr = (f_out)dlsym(h, "maete_encode_header");
    L.eenc = (f_out)dlsym(h, "maete_encode");
    L.ddec = (f_out)dlsym(h, "maetd_decode");
    L.epush = (f_push)dlsym(h, "maete_push");
    L.dclone = (f_clone)dlsym(h, "maetd_clone");
    L.dcfg = (f_cfg)dlsym(h, "maetd_config");
    if (!L.init || !L.ecreate || !L.dcreate || !L.ehdr || !L.eenc || !L.ddec || !L.epush || !L.dclone || !L.dcfg) {
        fprintf(stderr, "missing symbols in libmaet\n"); return 0;
    }
    L.init();
    return 1;
}

/* returns malloc'd RGBA, sets *w,*h; NULL on failure */
static uint8_t *decode(const uint8_t *spi, size_t n, int *w, int *h) {
    if (n < 8) return NULL;
    uint32_t n0, n1;
    memcpy(&n0, spi, 4);
    if ((size_t)n0 + 8 > n) return NULL;
    memcpy(&n1, spi + 4 + n0, 4);
    if ((size_t)n0 + 8 + n1 > n) return NULL;
    uint32_t cd[4] = {1, 0, 0, 0};
    void *dec = L.dcreate(cd, NULL);
    if (!dec) return NULL;
    uint8_t bitb[256] = {0}, stat[2048] = {0};
    put64(bitb, 0x20, (uint64_t)(uintptr_t)(spi + 4)); put32(bitb, 0x30, n0);
    L.ddec(dec, bitb, stat);
    put64(bitb, 0x20, (uint64_t)(uintptr_t)(spi + 8 + n0)); put32(bitb, 0x30, n1);
    L.ddec(dec, bitb, stat);
    int ww = 0, hh = 0, sz = 4;
    sz = 4; L.dcfg(dec, 0xc9, &ww, &sz);
    sz = 4; L.dcfg(dec, 0xca, &hh, &sz);
    if (ww <= 0 || hh <= 0 || ww > 8192 || hh > 8192) return NULL;
    uint8_t *out = calloc((size_t)ww * hh, 4);
    uint8_t imgb[0x150] = {0};
    put32(imgb, 0x20, ww); put32(imgb, 0x24, hh); put32(imgb, 0x44, hh);
    put32(imgb, 0x30, 500); put32(imgb, 0x34, ww * 4);
    put64(imgb, 0x58, (uint64_t)(uintptr_t)out);
    L.dclone(dec, imgb);
    *w = ww; *h = hh;
    return out;
}

static uint8_t *readf(const char *p, size_t *n) {
    FILE *f = fopen(p, "rb"); if (!f) return NULL;
    fseek(f, 0, SEEK_END); long s = ftell(f); fseek(f, 0, SEEK_SET);
    uint8_t *b = malloc(s > 0 ? s : 1);
    if (fread(b, 1, s, f) != (size_t)s) { fclose(f); free(b); return NULL; }
    fclose(f); *n = s; return b;
}
static int writef(const char *p, const void *b, size_t n) {
    FILE *f = fopen(p, "wb"); if (!f) return 0;
    int ok = fwrite(b, 1, n, f) == n; fclose(f); return ok;
}

static int cmd_dec(const char *in, const char *out) {
    size_t n; uint8_t *spi = readf(in, &n);
    if (!spi) { fprintf(stderr, "cannot read %s\n", in); return 2; }
    int w, h; uint8_t *px = decode(spi, n, &w, &h);
    if (!px) { fprintf(stderr, "decode failed\n"); return 3; }
    if (!writef(out, px, (size_t)w * h * 4)) return 4;
    printf("%d %d\n", w, h);
    return 0;
}

static int cmd_enc(const char *in, int w, int h, const char *out) {
    size_t n; uint8_t *px = readf(in, &n);
    if (!px || n != (size_t)w * h * 4) { fprintf(stderr, "bad rgba input\n"); return 2; }
    uint32_t cdsc[32] = {0};
    cdsc[0] = 1; cdsc[1] = w; cdsc[2] = h; cdsc[8] = 500;
    int err = 0;
    void *enc = L.ecreate(cdsc, &err);
    if (!enc) { fprintf(stderr, "maete_create failed (%d)\n", err); return 3; }
    uint8_t *ob = calloc(1, BUFSZ);
    uint8_t bitb[256], stat[512];
    uint8_t *hdr = NULL, *body = calloc(1, BUFSZ); size_t hl = 0, bl = 0;
    for (int step = 0; step < 10; step++) {
        memset(bitb, 0, sizeof bitb); memset(stat, 0, sizeof stat);
        put64(bitb, 0x20, (uint64_t)(uintptr_t)ob); put32(bitb, 0x30, BUFSZ); put32(stat, 0x14, 64);
        if (step == 0) {
            L.ehdr(enc, bitb, stat);
            int wr = get32(stat, 0);
            if (wr > 0 && (uint32_t)wr <= BUFSZ) { hdr = malloc(wr); memcpy(hdr, ob, wr); hl = wr; }
            uint8_t imgb[0x150] = {0};
            put32(imgb, 0x20, w); put32(imgb, 0x24, h); put32(imgb, 0x30, 500); put32(imgb, 0x34, w * 4);
            put64(imgb, 0x58, (uint64_t)(uintptr_t)px);
            L.epush(enc, imgb);
            continue;
        }
        L.eenc(enc, bitb, stat);
        int wr = get32(stat, 0);
        if (wr <= 0 || (uint32_t)wr > BUFSZ) break;
        if (bl + wr > BUFSZ) break;
        if (bl && memmem(body, bl, ob, wr)) break;
        memcpy(body + bl, ob, wr); bl += wr;
    }
    if (!hdr || !bl) { fprintf(stderr, "encoder produced no output\n"); return 4; }
    size_t tot = 8 + hl + bl;
    uint8_t *spi = malloc(tot);
    uint32_t a = (uint32_t)hl, b = (uint32_t)bl;
    memcpy(spi, &a, 4); memcpy(spi + 4, hdr, hl); memcpy(spi + 4 + hl, &b, 4); memcpy(spi + 8 + hl, body, bl);
    int w2, h2; uint8_t *chk = decode(spi, tot, &w2, &h2);
    int exact = chk && w2 == w && h2 == h && memcmp(chk, px, n) == 0;
    if (!exact) { fprintf(stderr, "verify MISMATCH\n"); return 5; }
    if (!writef(out, spi, tot)) return 6;
    printf("OK %zu bytes EXACT\n", tot);
    return 0;
}

int main(int argc, char **argv) {
    if (argc >= 4 && !strcmp(argv[1], "dec")) { if (!load()) return 1; return cmd_dec(argv[2], argv[3]); }
    if (argc >= 6 && !strcmp(argv[1], "enc")) { if (!load()) return 1; return cmd_enc(argv[2], atoi(argv[3]), atoi(argv[4]), argv[5]); }
    fprintf(stderr, "usage: spitool dec in.spi out.rgba | enc in.rgba w h out.spi\n");
    return 1;
}

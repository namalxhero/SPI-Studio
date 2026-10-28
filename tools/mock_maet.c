#include <stdint.h>
#include <string.h>
#include <stdlib.h>
static int W, H, calls; static const uint8_t *pix; static const uint8_t *payload; static int dcalls;
static void put32(uint8_t *b, size_t o, uint32_t v) { memcpy(b + o, &v, 4); }
static uint64_t g64(const uint8_t *b, size_t o) { uint64_t v; memcpy(&v, b + o, 8); return v; }
static int32_t g32(const uint8_t *b, size_t o) { int32_t v; memcpy(&v, b + o, 4); return v; }
int maet_init(void) { return 0; }
void *maete_create(void *cdsc, int *err) { uint32_t *c = cdsc; if (c[0] != 1) { if (err) *err = -103; return 0; } W = c[1]; H = c[2]; calls = 0; return (void *)&W; }
int maete_encode_header(void *e, uint8_t *bitb, uint8_t *stat) { uint8_t *o = (uint8_t *)(uintptr_t)g64(bitb, 0x20); memset(o, 0xAA, 21); put32(o, 6, W); put32(o, 10, H); put32(stat, 0, 21); return 0; }
int maete_push(void *e, uint8_t *imgb) { pix = (const uint8_t *)(uintptr_t)g64(imgb, 0x58); return 0; }
int maete_encode(void *e, uint8_t *bitb, uint8_t *stat) {
    uint8_t *o = (uint8_t *)(uintptr_t)g64(bitb, 0x20);
    if (calls++ > 0) { put32(stat, 0, 0); return 0; }
    put32(o, 0, W); put32(o, 4, H); memcpy(o + 8, pix, (size_t)W * H * 4); put32(stat, 0, 8 + W * H * 4); return 0;
}
void *maetd_create(void *cd, void *err) { dcalls = 0; return (void *)&H; }
int maetd_decode(void *d, uint8_t *bitb, uint8_t *stat) {
    const uint8_t *a = (const uint8_t *)(uintptr_t)g64(bitb, 0x20);
    if (dcalls++ == 0) { W = g32(a, 6); H = g32(a, 10); } else { payload = a; }
    return 203;
}
int maetd_config(void *d, int id, int *v, int *sz) { *v = id == 0xc9 ? W : H; return 0; }
int maetd_clone(void *d, uint8_t *imgb) { uint8_t *o = (uint8_t *)(uintptr_t)g64(imgb, 0x58); memcpy(o, payload + 8, (size_t)W * H * 4); return 0; }

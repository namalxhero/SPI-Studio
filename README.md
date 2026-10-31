# SPI Studio

Decode / encode Samsung `.spi` images (MAET codec, used for the off-charging animation in `/system/media`)
on a rooted or stock phone. The app drives the device's own `/system/lib64/libmaet.so` through a tiny native
helper (`native/spitool.c`), so no codec is reimplemented.

* Decode: pick a `.spi` (or scan `/system/media`) -> preview -> save PNG
* Encode: pick a PNG -> lossless `.spi` (verified by decoding it back) -> save

Build: push to GitHub, the workflow compiles `spitool` with the NDK, then Gradle builds the APK
(artifact `SPI-Studio-debug`).

## UI
* 10 living nature moods (forest, sakura, aurora, ocean, sunset, stars, autumn, rain, snow, fireflies),
  one per minute with a slow 6 s cross-fade. Moods change on their own, nothing to tap.
* Liquid-glass bubble buttons with custom icons (no icon libraries).

## Music
Put your song at `app/src/main/res/raw/intro.mp3` (exact lowercase name). It loops with a slow fade-in;
the "Music on/off" bubble appears only when the file exists.

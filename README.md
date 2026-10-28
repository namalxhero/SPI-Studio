# SPI Studio

Decode / encode Samsung `.spi` images (MAET codec, used for the off-charging animation in `/system/media`)
on a rooted or stock phone. The app drives the device's own `/system/lib64/libmaet.so` through a tiny native
helper (`native/spitool.c`), so no codec is reimplemented.

* Decode: pick a `.spi` (or scan `/system/media`) -> preview -> save PNG
* Encode: pick a PNG -> lossless `.spi` (verified by decoding it back) -> save

Build: push to GitHub, the workflow compiles `spitool` with the NDK, then Gradle builds the APK
(artifact `SPI-Studio-debug`).

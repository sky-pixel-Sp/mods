# SkyLyrics — by Sky | Minecraft Java 26.3

**SkyLyrics 0.9.2** is a client-side Fabric mod by **Sky**. It includes an in-game Music Center, offline music library, browser-connected media controls, synchronized lyrics, configurable 2D / 3D HUD, cinematic sky presets, and a custom mod icon plus an in-game gallery of authentic NASA/JPL nebula photography used by the Skies selector.

## Quick start

1. Launch Minecraft Java **26.3** with Fabric Loader and the matching Fabric API.
2. Put `skylyrics-0.9.2.jar` in that Fabric profile's `mods` folder.
3. Enter a world and press **O** to open SkyLyrics.
4. For local music, choose **Music Center → Import Song** and select MP3, OGG, or WAV.
5. For synchronized local lyrics, open **Lyrics → Import / Match LRC** and select the matching `.lrc` file.
6. For browser music, choose **Online Music**, open Spotify Web / YouTube Music, then install the included Chrome / Edge extension.

The in-game **Quick Guide** tab repeats these steps and links directly to Music Center, Lyrics, and Online Music. All major controls are available through clickable menus; no chat commands are required.

## Packaged resources

The final JAR is verified for the declared Sky author and icon, eight embedded TTF fonts with OFL notices, 20 sky post-effect presets, four shaders, five nested audio codecs, and eight authentic NASA/JPL 1600×900 photo previews used by the Skies selector. Full-resolution source JPGs are fetched and validated during the build, then resized for runtime. The 0.9.2 JAR measures 7,874,042 bytes and the build enforces a 5,000,000-byte minimum.

## Build and test

Open the **Actions** tab or create a pull request to `main` to run **Build SkyLyrics for Minecraft 26.3**. The workflow compiles with Java 25, runs tests, verifies required assets inside the final JAR, validates the browser extension, and uploads a complete **SkyLyrics-Minecraft-26.3** package containing the JAR, README, and Chrome / Edge extension folder.

A green CI build is not a substitute for in-game testing. Local audio output, browser controls, lyric synchronization, all sky presets, and HUD placement still need a manual smoke test in TLauncher.

Panduan cepat berbahasa Indonesia tersedia di [`SkyLyrics-26.3/PANDUAN-ID.md`](SkyLyrics-26.3/PANDUAN-ID.md). Dokumentasi lengkap tersedia di [`SkyLyrics-26.3/README.md`](SkyLyrics-26.3/README.md).

# SkyLyrics — Minecraft Java 26.3

**SkyLyrics 0.6.0 — Music Center** adds a clickable in-game music UI, offline audio playback, browser-connected online music controls, synchronized lyrics, and optional cinematic sky effects.

## Main features

- **Offline:** import downloaded MP3, OGG, WAV, AIFF or AU files into the local library; play them without internet; import matching `.lrc` files for synchronized lyrics.
- **Online:** open YouTube Music or Spotify Web from the in-game UI, then control the browser player and receive metadata through the included Chrome/Edge extension.
- **Clickable UI:** Music Center, Offline Library, Online Music, Lyrics Studio, Styles, Skies, HUD, Keybinds and Performance pages. No chat command is required.
- **Configurable controls:** O, H, K, N and B are defaults; rebind them in Minecraft Options → Controls → Key Binds → SkyLyrics.
- **Optional shader:** audio and lyrics keep working if the sky effect is disabled.

## Build on GitHub

Open the **Actions** tab and run **Build SkyLyrics for Minecraft 26.3**, or open a successful workflow run for branch `skylyrics-0.6-music-center`. Download the artifact **SkyLyrics-Minecraft-26.3** and extract `skylyrics-0.6.0.jar`.

Do not treat a JAR as verified until the workflow completes successfully. After CI, perform the local audio and browser-control smoke tests in TLauncher.

## Install

Use Minecraft Java 26.3, Fabric Loader 0.20.1+ and Fabric API 0.165.2+26.3. Put the SkyLyrics JAR and Fabric API JAR in the same client `mods` folder.

Enter a world and press **O** to open Music Center. The mod is client-side; the multiplayer server does not need it.

See [`SkyLyrics-26.3/README.md`](SkyLyrics-26.3/README.md) for offline/online instructions, supported formats, keybinds, troubleshooting and licensing.
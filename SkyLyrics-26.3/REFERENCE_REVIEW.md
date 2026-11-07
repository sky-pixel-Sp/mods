# External reference review: VerseFall 1.1.0

## Uploaded artifact inspected

- File reviewed: `versefall-1.1.0-mc1.19.4.jar`.
- Embedded Fabric metadata: mod id `spotify_mod`, display name `VerseFall`, version `1.0.0+mc1.19.4`.
- Embedded metadata pins Minecraft to `1.19.4`, requires Fabric Loader `>=0.14.19`, and Java `>=17`.
- The artifact contains Spotify/media, lyrics, keybind, HUD/editor, glass UI, visualizer, island/now-playing and music-note feature areas, plus unrelated combat/cosmetic modules.
- This binary is not compatible with the SkyLyrics target runtime (Minecraft Java 26.3 / Fabric Loader 0.20.1+). It must not be added as a nested dependency or placed inside the SkyLyrics JAR.

## Licensing caution

The uploaded JAR's `fabric.mod.json` declares MIT, but the corresponding CurseForge project/file page currently displays **All Rights Reserved**. No standalone LICENSE text was found inside the uploaded archive. These signals conflict, so the project treats the uploaded JAR as a behavior/UI reference only and does not copy, redistribute, decompile, or bundle its classes, shaders, fonts, icons, or other assets.

## SkyLyrics implementation direction

SkyLyrics implements its own code and assets for the requested feature categories:

- Clickable Music Center and keybinds.
- Local offline audio library and player.
- Browser-based online playback controls with metadata-only local bridge.
- Synchronized LRC lyrics, HUD and floating world text.
- Now Playing artwork pill, sampled artwork accent/glow and offline PCM-reactive waveform.
- Local listening history and statistics.
- Optional procedural sky effects.

The SkyLyrics code is a separate implementation. Similar user-facing functionality does not imply code or asset reuse.

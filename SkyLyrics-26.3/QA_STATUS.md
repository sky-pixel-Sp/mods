# QA STATUS — SkyLyrics 0.6.0 / Music Center

## Added in this branch

- Clickable Music Center, Offline Library, Online Music, and Keybinds tabs.
- Local Java Sound playback engine with play/pause, stop, seek, volume, and background streaming from disk.
- Local library import/copy for MP3, OGG, WAV, AIFF and AU; matching LRC discovery by basename.
- Browser extension command polling for play/pause, pause, next, previous, seek and volume; command endpoint is loopback-only and extension-origin checked.
- Configurable Minecraft key mappings for menu, lyric visibility, playback, next and previous.
- Mini-player HUD support for local offline audio.
- Local-only listening history and stats for offline selections and observed online playback sessions; latest 100 sessions persist in `history.json`.
- Offline mini-player waveform uses measured PCM amplitude; online mode remains a transport animation because audio is not captured.
- Now Playing can sample the current allow-listed cover artwork's accent color for a subtle optional album glow.
- Unit tests for history persistence, unique recent-track counting, bounded entries and clearing.
- Unit tests for library import, invalid extensions, duplicate filenames, and matching LRC.
- LGPL notices for bundled MP3SPI and VorbisSPI decoders.

## Release gates — not yet verified

- [x] GitHub Actions `clean test build` succeeds with JDK 25 (Run #92, commit `c6b41e3`).
- [x] Built JAR includes LocalAudioEngine, MusicLibrary, MusicHistory and bundled MP3SPI / VorbisSPI nested dependencies (verified by the release workflow).
- [ ] TLauncher Minecraft 26.3 can import and play a downloaded MP3 without internet.
- [ ] OGG and WAV playback are checked separately.
- [ ] Matching `<song name>.lrc` is synchronized to local playback and lyrics stop at end of track.
- [ ] UI Previous / Next / Seek / Volume controls work and the library remains available after game restart.
- [ ] YouTube Music opens in the browser, connects through the extension, and accepts the Minecraft UI play/pause/next/previous/seek/volume controls.
- [ ] History & Stats counts a new track once per playback session and persists after restarting the game.
- [ ] Clear History clears local metadata without stopping playback.
- [ ] Offline waveform responds to real PCM level changes; online waveform does not claim to be an audio analyser.
- [ ] Artwork glow uses the current cover accent when artwork is available and falls back cleanly when it is absent.
- [ ] Spotify Web metadata and best-effort controls work; no Spotify lyric sync is exposed.
- [ ] Minecraft Options → Controls can rebind all SkyLyrics key mappings.
- [ ] Disabling the sky shader does not stop local or online music.
- [ ] Multiplayer server does not require SkyLyrics or a resource pack.

CI success is not an in-game audio test. Do not publish a verified release before the manual TLauncher checks pass.
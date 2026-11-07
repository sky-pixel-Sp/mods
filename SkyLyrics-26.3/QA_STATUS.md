# QA STATUS — SkyLyrics 0.7.0 / Karaoke Timing & HUD Layout Studio

## Added in this branch

- Independent VerseFall technical review documenting its original class/package architecture, Spotify OAuth/PKCE flow, Windows media-session reader, LRCLIB word timing, draggable HUD system, graphics stack, compatibility and licensing caveats. This is analysis only; VerseFall code/assets are not bundled or copied.
- LRC/LRCLIB inline per-word timestamps (`<mm:ss.xx>`) parsed into word timing metadata while preserving the original line-timed LRC behavior.
- Karaoke HUD highlighting driven by per-word timestamps; 3D lyrics reveal words according to their actual word timestamps when supplied.
- In-game HUD Layout Studio with independent drag positions for Now Playing and Lyrics HUD.
- Normalized persisted positions that survive resolution changes, double-click per-panel reset, right-click drag cancel, and Reset All.
- Config version 3 migration adds position fields without overwriting existing custom sky presets or lyrics vertical position.

- Clickable Music Center, Offline Library, Online Music, and Keybinds tabs.
- Local Java Sound playback engine with play/pause, stop, seek, volume, and background streaming from disk.
- Local library import/copy for MP3, OGG, WAV, AIFF and AU; matching LRC discovery by basename.
- Browser extension command polling for play/pause, pause, next, previous, seek and volume; command endpoint is loopback-only and extension-origin checked.
- Configurable Minecraft key mappings for menu, lyric visibility, playback, next and previous.
- In-game keybinding editor: click one of five key rows, press a keyboard key or mouse button, and use ESC to cancel capture.
- One-click restore of the default O / H / K / N / B shortcuts; changed mappings are saved through Minecraft Options.
- Mini-player HUD support for local offline audio.
- Local-only listening history and stats for offline selections and observed online playback sessions; latest 100 sessions persist in `history.json`.
- Offline mini-player waveform uses measured PCM amplitude; online mode remains a transport animation because audio is not captured.
- Now Playing can sample the current allow-listed cover artwork's accent color for a subtle optional album glow.
- Unit tests for history persistence, unique recent-track counting, bounded entries and clearing.
- Unit tests for library import, invalid extensions, duplicate filenames, and matching LRC.
- LGPL notices for bundled MP3SPI and VorbisSPI decoders.

## Release gates — not yet verified

- [ ] A word-synced LRCLIB result highlights the active word at the correct timestamp in the screen HUD.
- [ ] Word-synced 3D lyrics reveal words on their supplied timestamps; standard LRC keeps its line-progress fallback.
- [ ] HUD Layout Studio can move Now Playing and Lyrics independently with mouse drag.
- [ ] Double-click resets only the selected panel; right-click cancels without leaving the preview position changed.
- [ ] HUD positions persist after game restart and stay within the visible screen after changing resolution.
- [ ] Config version 2 migrates to version 3 while preserving user-selected sky preset, lyric style and vertical position.

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
- [ ] In-game KEYBINDS page can rebind all five SkyLyrics shortcuts with keyboard input.
- [ ] In-game KEYBINDS page can assign mouse buttons and cancel capture with ESC.
- [ ] Restore Defaults restores O / H / K / N / B and persists after restarting the game.
- [ ] Minecraft Options → Controls reflects the in-game keybinding changes.
- [ ] Disabling the sky shader does not stop local or online music.
- [ ] Multiplayer server does not require SkyLyrics or a resource pack.

CI success is not an in-game audio or visual-interaction test. Do not publish a verified release before the manual TLauncher checks pass.
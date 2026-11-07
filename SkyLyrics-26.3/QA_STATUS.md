# QA STATUS — SkyLyrics 0.8.0 / Modular HUD Studio

## Added in 0.8.0
- Five independently movable client HUD cards: Now Playing, Lyrics, PCM Visualizer, Recently Played, and Listening Stats.
- Per-widget normalized X/Y positions persisted in config schema version 4; migration from older settings retains existing v3 layout and preferences.
- HUD Layout Studio supports left-drag, release-to-save, right-click drag cancel, double-click single-widget reset, Reset All, and visibility switches for optional cards.
- Dedicated visualizer uses measured PCM amplitude from decoded local audio. Browser integrations are labelled transport-only and never claim to capture browser audio.
- Recently Played card reads the existing local history and shows the latest three sessions.
- Listening Stats displays total plays and unique tracks represented in the bounded recent history.
- Unit tests cover v4 widget defaults, persisted positions, visibility flags, and normalized position clamping.

## Release gates
- [x] Existing v3 position and preference fields remain loaded by the migration path.
- [x] New widget positions and visibility settings are saved and restored.
- [x] Unit tests cover widget state persistence and clamping.
- [ ] Manual in-game TLauncher 26.3 verification of all five widget positions and overlap handling.
- [ ] Manual verification of drag, release-save, right-click cancel, double-click reset, Reset All, and visibility switches.
- [ ] Verify PCM bars react to a real local MP3/OGG/WAV in-game; browser mode must show transport-only labelling.
- [ ] Verify Recently Played and Listening Stats update after playback and persist after restart.
- [ ] Full clean test build, packaged JAR inspection, and size comparison against 0.7.0.
- [ ] Manual audio, online-browser bridge, and visual-effects smoke tests remain required.

CI success confirms compilation, automated tests, resource verification and packaging only. It does not confirm mouse interaction or audio output inside TLauncher.

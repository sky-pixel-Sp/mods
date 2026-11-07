# QA STATUS — SkyLyrics 0.8.0 / Modular HUD Studio

## Added in 0.8.0
- Five independently movable client HUD cards: Now Playing, Lyrics, PCM Visualizer, Recently Played, and Listening Stats.
- Per-widget normalized X/Y positions persisted in config schema version 4; migration from older settings retains existing v3 layout and preferences.
- HUD Layout Studio supports left-drag, release-to-save, right-click drag cancel, double-click single-widget reset, Reset All, and visibility switches for optional cards.
- Dedicated visualizer uses measured PCM amplitude from decoded local audio. Browser integrations are labelled transport-only and never claim to capture browser audio.
- Recently Played card reads the existing local history and shows the latest three sessions.
- Listening Stats displays total plays and unique tracks represented in the bounded recent history.
- Unit tests cover v4 widget defaults, persisted positions, visibility flags, and normalized position clamping.

## Automated release checks
- [x] Existing v3 position and preference fields remain loaded by the migration path.
- [x] New widget positions and visibility settings are saved and restored.
- [x] Unit tests cover widget state persistence and clamping.
- [x] GitHub Actions Run #110 completed clean test build successfully with JDK 25.
- [x] Browser-extension validation and packaged-JAR resource/class assertions passed.
- [x] Packaged JAR contains SkyLyricsHudWidgets, the upgraded HudEditorScreen, existing renderers, licenses, fonts, shaders, and nested audio codecs.
- [x] Size and SHA-256 of the downloaded binary JAR were checked.

## Verified build artifact
- Workflow run: https://github.com/sky-pixel-Sp/mods/actions/runs/42369436559
- Artifact name: SkyLyrics-Minecraft-26.3 (artifact ID 13572619825, expires 2026-12-07).
- Binary: skylyrics-0.8.0.jar, 1,873,791 bytes.
- Binary SHA-256: 116aa547e54aa77f2ddb4c7857a1bb6014aea96d32ebb5befa9d1724ad42a6bf.
- Source JAR: skylyrics-0.8.0-sources.jar, 1,412,024 bytes.
- Artifact ZIP: 3,214,180 bytes (binary + sources).
- Artifact ZIP SHA-256: b44e7f24898f941dc9aa07a9ef7e1bc131d3be4f2b636da1f3710988ded301a1.

## Manual release gates — still required
- [ ] Manual in-game TLauncher 26.3 verification of all five widget positions and overlap handling.
- [ ] Manual verification of drag, release-save, right-click cancel, double-click reset, Reset All, and visibility switches.
- [ ] Verify PCM bars react to a real local MP3/OGG/WAV in-game; browser mode must show transport-only labelling.
- [ ] Verify Recently Played and Listening Stats update after playback and persist after restart.
- [ ] Manual audio, online-browser bridge, and visual-effects smoke tests remain required.

CI success confirms compilation, automated tests, resource verification and packaging only. It does not confirm mouse interaction or audio output inside TLauncher.

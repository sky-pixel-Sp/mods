# QA STATUS — SkyLyrics 0.11.0 / Minecraft 26.3

## 0.11.0 feature implementation — 2026-11-08

### Added to the local-only Music Center
- Offline Library supports case-insensitive filename search.
- All / Favorites / Playlist queue filters are persisted locally.
- Users can favorite individual tracks and create, cycle, add/remove tracks from, and delete named playlists.
- Shuffle selects a different track for manual next/previous and automatic end-of-track advancement when multiple queued tracks exist.
- Repeat Off stops at the end of the active queue; Repeat All wraps to the first queued track; Repeat One restarts the finished track.
- Queue preferences, playlist membership, favorites, and active filter are stored as metadata in `config/skylyrics/library-state.json`. No audio bytes or account identifiers are written to this file.
- Track references are restricted to safe file names inside the SkyLyrics music directory. Playlist names reject separators, control characters, empty values, and overly long names.
- Added JUnit coverage for favorites persistence, playlist persistence and membership, unsafe names, shuffle/repeat persistence, and search matching.

### Build verification status
- Development branch: `skylyrics-0.11.0-playlists-queue`.
- Version property: `mod_version=0.11.0`.
- CI verification is pending until the pull-request workflow completes. Do not treat this entry as a successful build record until the run is inspected.
- Required pipeline: JDK 25 / Gradle 9.8.2 / `clean test build`, JAR metadata/resource checks, and complete package assembly.

### Manual validation still required
- No live Minecraft 26.3 or TLauncher client test has yet been completed for this branch.
- No actual laptop audio output has been tested here.
- No real in-game mouse/keyboard acceptance test has been performed for playlist creation, filtering, favorite toggles, search, or queue transitions.
- Verify with a short local test library: All → Favorites → each playlist; search; add/remove a track; close and restart Minecraft; verify metadata persistence.
- Verify Repeat Off / All / One and Shuffle across end-of-track transitions using real audio files.
- Client-local only: a multiplayer server and other players do not receive or hear the user's locally played music.

### Scope and compatibility
- Retains the 0.10.0 local-only design: no Spotify, YouTube, browser extension, browser listener, native file dialog, or external media player is part of the intended playback flow.
- Target: Minecraft Java 26.3, Fabric Loader 0.20.1+, Fabric API 0.165.2+26.3, Java 25+.
- Audio formats remain MP3, OGG, WAV, AIFF, and AU.

---

# QA STATUS — SkyLyrics 0.10.0 / Minecraft 26.3

## Current local-only build — 2026-11-08

### Source and CI evidence

- Development branch: `skylyrics-0.10.0-local-in-game`.
- Commit verified by CI: `5ba433e28233981a16cce4d2bd344ad8637152ef`.
- GitHub Actions: [Run #219](https://github.com/sky-pixel-Sp/mods/actions/runs/42440444857).
- Result: **success** — JDK setup, Gradle setup, `clean test build`, JAR/resource checks, install-package assembly, and artifact upload all completed.
- Toolchain: JDK 25 and Gradle 9.8.2.
- Minecraft target: `~26.3`; Fabric Loader `>=0.20.1`; Java `>=25`. Build property pins Fabric API `0.165.2+26.3`.
- Verified JAR: `skylyrics-0.10.0.jar`, **7,852,748 bytes**.
- JAR SHA-256: `713887cbab5fd2671f1acfe260e71fb550d9a64f6e55fae8f421fd01ee387a05`.
- GitHub install artifact: `SkyLyrics-Minecraft-26.3`, artifact ID `13600793822`, **7,831,532 bytes**.
- Artifact ZIP SHA-256: `f1d15b01e82d7525027a2d79dc880d7d7894a508438707cbdd8a192d2e9b916a`.
- Artifact expiry: **2026-12-08**.
- Package contains the mod JAR, README, Indonesian guide, changelog, QA report, and MIT license. It does not contain a browser extension.

### Local-only package inspection

The downloaded JAR and package were inspected after the successful CI run.

- Fabric metadata reports mod version `0.10.0`, Minecraft `~26.3`, Java `>=25`, and client-only environment.
- Present: `InGameFilePickerScreen`, `LocalAudioEngine`, `PlaybackSessionGuard`, `SkyLyricsScreen`, `HudEditorScreen`, five bundled audio codec JARs, eight TTF font files, 20 sky presets, four shaders, and eight credited photo previews.
- Absent: `BrowserMediaServer`, `MediaStatus`, `LyricsLookupClient`, and `AlbumArtManager` classes.
- The former browser extension source files were removed from the 0.10.0 branch. The workflow no longer validates or packages browser-extension files.
- The client controller does not start a browser listener or launch Spotify / YouTube. The Music Center has no Online Music tab.
- Import UI is implemented as an in-game file browser. The code does not invoke a native Windows file dialog or an external media player.
- Music Center, the file browser, and HUD Layout Studio declare non-pausing screen behavior.

### What CI verifies

- Java/Fabric source compilation for Minecraft 26.3.
- Gradle unit-test task.
- Sky resource, font, shader, artwork, nested codec, and final-JAR metadata/contents checks.
- Required local-only UI/audio classes are present; the retired browser integration classes are absent.
- The complete install package assembles and uploads successfully.

### Not manually verified

- No live Minecraft 26.3 or TLauncher launch was performed in this environment.
- No audible output was tested through the user's laptop audio device.
- The in-game file browser has not been mouse-tested on the user's actual Windows folders.
- Play/Pause, seek, volume, LRC display, HUD positioning, multiplayer-screen behavior, and shutdown/cleanup still require hands-on client testing.
- A successful CI build is not proof that a specific laptop's audio device or codec providers will behave correctly at runtime.

### Scope limitations in 0.10.0

- Audio is client-local. Other players on a multiplayer server do **not** hear the local music; no server audio broadcast or synchronization protocol is implemented.
- Local audio formats supported by the current importer: MP3, OGG, WAV, AIFF, and AU.
- The current library is a local track list with pagination. A full playlist editor, favorites, shuffle, repeat modes, and library search are not part of this build.
- The HUD editor supports moving five widgets, toggling optional cards, and resetting positions. Resize handles and multiple named layout presets are not implemented.
- The in-game file browser reads local directories that the Minecraft process can access; normal operating-system file permissions still apply.

---

# QA STATUS — SkyLyrics 0.9.3 / Minecraft 26.3

## 0.9.3 automated verification — 2026-11-08

<details>
<summary>Build and artifact evidence</summary>

- Branch: `skylyrics-0.9.3-audio-session-stability`
- Source revision tested by Run #157: `5ee98f42dad7115017675ce2b178e4b456b0e4ca`
- GitHub Actions: [Run #157](https://github.com/sky-pixel-Sp/mods/actions/runs/42435751000)
- Pull request: [#12 — SkyLyrics 0.9.3](https://github.com/sky-pixel-Sp/mods/pull/12)
- Run conclusion: **success**; all eight build, test, validation, package, and upload steps completed.
- Build command: `gradle --no-daemon clean test build`
- Toolchain: JDK 25 and Gradle 9.8.2.
- Browser extension: `node --check` passed for `content.js` and `background.js`; manifest JSON validation passed.
- Final JAR verification reported 8 fonts, 20 sky presets, 4 shaders, 5 nested audio codecs, and 8 NASA/JPL 1600×900 previews.
- Verified JAR: `skylyrics-0.9.3.jar`, **7,875,567 bytes**.
- JAR SHA-256: `371bdf32552eb326a08d1a171427dda33fffdcc03f1226a7200d977c4fc5d4ef`.
- Fabric metadata version: `0.9.3`; Minecraft dependency: `~26.3`; Java dependency: `>=25`.
- GitHub Actions package artifact: `SkyLyrics-Minecraft-26.3`, artifact ID `13599532663`, archive size **7,851,745 bytes**, SHA-256 `7b5501ed284c76a4a6bb010d72204c8ec84568f2d31a9190c7249bb1bbbc0817`.
- Artifact expires on **2026-12-08**.

</details>

## 0.9.3 audio lifecycle changes

- Each local playback worker receives a unique generation token.
- Replacing or stopping a track invalidates the previous session before stale work can update state.
- A worker only closes the `SourceDataLine` instance that it created.
- Stop no longer synchronously joins a codec worker from the Minecraft client thread.
- A paused worker checks pending seek requests again after waking.
- Added four JUnit regression tests for active-session creation, session replacement, invalidation, and stale-token rejection. The Gradle `test` task passed in Run #157.

## Not manually verified

- No Minecraft 26.3 client or TLauncher launch test was performed in this environment.
- No audible output test was performed against a real audio device. The previous user-reported Offline Library visibility / playback problem therefore remains a manual acceptance gate; the concurrency hardening is not proof that the full playback issue is resolved.
- Chrome / Edge extension loading, actual YouTube Music / Spotify browser sessions, live seek/volume behavior, Elytra rendering, and HUD mouse interactions were not tested in a running game.
- Do not publish a stable release until these manual checks are completed.

## Requested feature-scope gaps found in source audit

- The current library lists and paginates local audio files, but a complete playlist editor, favorites, shuffle, repeat, and library search were not found in the audited UI / `MusicLibrary` implementation.
- The HUD Layout Studio supports moving five widget cards, visibility toggles for optional cards, and reset actions. Resize handles, saved layout presets, and multiple named layouts were not found in the audited implementation.
- These are outstanding feature requests, not verified 0.9.3 features.

---

## Historical QA — SkyLyrics 0.9.2 / Minecraft 26.3

## 0.9.2 gallery artwork, bilingual UI, and packaging checks
- The build downloads and validates eight full-resolution NASA/JPL source JPGs, then creates eight 1600×900 photo previews. Only the eight previews actually used by the Skies selector ship in the runtime JAR; full-resolution source masters remain build-time inputs.
- Every photograph has a retained image-page URL and individual credit line in `META-INF/licenses/skylyrics-artwork/NOTICE.txt`; no NASA/JPL endorsement is implied.
- Skies now shows a photographic preview of the currently selected preset. The procedural post-effect remains the actual Minecraft sky effect.
- The HUD Layout Studio uses real album artwork when it is available and localizes its controls and helper text.
- The UI navigation now computes a smaller row pitch for short windows so all 13 tabs remain inside the panel.
- Main HUD cards, status labels, and recent-activity text follow the saved English / Indonesian language setting.

## 0.9.2 branding, navigation, and final-package checks
- Fabric metadata author is exactly `Sky`; the mod now declares its custom `assets/skylyrics/icon.png` icon.
- Added an Indonesian in-game PANDUAN CEPAT with direct buttons to Music Center, Lyrics, and Online Music.
- The GitHub artifact now bundles the JAR, installation README, MIT license, and the complete Chrome / Edge extension folder.
- Fixed navigation actions that previously opened a destination screen and then immediately refreshed the old tab; tab navigation and HUD editor navigation now use dedicated non-refreshing actions.
- Added a final-JAR verification task: author and Minecraft/Java metadata, icon, all eight TTF fonts and OFL notices, 20 sky presets, four shaders, and all five bundled audio codec JARs must be present.
- The JAR is checked for complete contents and must be at least 5,000,000 bytes. Verified 0.9.2 output: 7,874,042 bytes; no padding or duplicate assets. Runtime assets and dependencies must be genuine and loadable.

### Still required on the target PC
- [ ] Open Quick Guide, navigate between tabs, and open the HUD Layout Studio.
- [ ] Import local MP3 / OGG / WAV, confirm the Offline Library lists the file, and verify sound output.
- [ ] Load matching `.lrc` and confirm line / word synchronization.
- [ ] Test the browser extension with YouTube Music and Spotify Web.
- [ ] Test all 20 sky presets, text styles, Elytra lyrics, and HUD positioning in TLauncher.

---

## Historical QA — SkyLyrics 0.9.1 / Minecraft 26.3

The 0.9.1 release-candidate checks are preserved below for audit history.

## Historical QA — SkyLyrics 0.9.0 / Minecraft 26.3

## 0.9.0 port additions (implementation in review)
- Elytra Lyrics Mode moves 3D lyrics farther forward and keeps them camera-facing while the player is gliding; toggle is available in HUD settings and is persisted in config schema v5.
- Now Playing can render in Bordered, Borderless, or Compact style; choice is persisted and can be cycled from Styles.
- Lyrics page exposes -250 ms, +250 ms, and Reset sync-offset controls for the currently loaded track.
- Project branding and mod identity remain SkyLyrics (`skylyrics`); the third-party Lyrics project is a feature reference only, not a dependency or bundled JAR.

## Reference and licensing
- User-provided reference file 9041952 is `lyricsplus-3.0.0-1.20.1.jar`, for Minecraft 1.20.1.
- The same CurseForge project has a separate `lyricsplus-3.0.0-26.2.jar` release (file 9041918), but the reviewed project file list does not show a 26.3 release.
- CurseForge lists the reference project under MIT. No class, asset, or binary from the reference JAR is bundled in SkyLyrics 0.9.0; the implementation uses SkyLyrics code and the public feature description.
- The referenced mod monitors external PC/browser music players. SkyLyrics' offline player remains independent and must continue to play local tracks inside Minecraft without launching external media players.

## Manual checks for 0.9.0 — still required
- [ ] Enter an Elytra flight with World Lyrics enabled; verify the lyrics remain in front of the camera and do not remain behind the player.
- [ ] Toggle Elytra Lyrics Mode off; verify the normal world-lyrics position returns.
- [ ] Cycle Bordered → Borderless → Compact; verify layout, cover art, title and progress do not clip at common resolutions.
- [ ] Adjust sync offset by -250 ms / +250 ms and Reset; verify the active lyric line changes at the expected time.
- [ ] Confirm config v4 migrates to v5 and new settings persist after restart.
- [ ] Retest native local audio import, visible library listing, MP3/OGG/WAV output, and transport controls in TLauncher 26.3. This remains blocked by the user's earlier report until directly retested.

---

# QA STATUS — SkyLyrics 0.8.0 / Modular HUD Studio

## Added in 0.8.0
- Five independently movable client HUD cards: Now Playing, Lyrics, PCM Visualizer, Recently Played, and Listening Stats.
- Per-widget normalized X/Y positions persisted in config schema version 4; migration from older settings retains existing v3 layout and preferences.
- HUD Layout Studio supports left-drag, release-to-save, right-click drag cancel, double-click single-widget reset, Reset All, and visibility switches for optional cards.
- Dedicated visualizer uses measured PCM amplitude from decoded local audio. Browser integrations are labelled transport-only and never claim to capture browser audio.
- Recently Played card reads the existing local history and shows the latest three sessions.
- Listening Stats displays total plays and unique tracks represented in the bounded recent history.
- Unit tests cover v4 widget defaults, persisted positions, visibility flags, and normalized position clamping.

## Native in-game music requirement — user-reported blocker (2026-11-07)
The user confirms that the SkyLyrics menu opens in Minecraft 26.3, but reports that the song list is not appearing and audio is not playing as expected. This is an observed manual-test failure and blocks declaring local playback verified.

Required behavior:
- Music playback must run natively while Minecraft remains open, through SkyLyrics' in-process audio engine and the system audio output.
- Do not launch Spotify, YouTube Music, VLC, a browser, or another third-party media player for local/offline playback.
- Imported audio files must appear as selectable rows in the in-game Offline Library.
- Selecting a track must start playback inside Minecraft and update title, playback status, elapsed time, duration, and visualizer.
- Play/Pause, Stop, Previous, Next, Seek, and Volume must operate on the local in-game player.
- The native OS file picker may be used only to choose a local file for import; it must not be used as the playback engine.

Acceptance checks for the next fix:
- Import MP3, OGG, WAV, AIFF, and AU files and confirm each supported file is copied into config/skylyrics/music/ and shown in Offline Library after the screen is reopened.
- Select a listed track and confirm audible output while Minecraft remains the foreground application, with no external media player launched.
- Verify Pause/Resume, Stop, Previous/Next, Seek, Volume, Now Playing metadata, and PCM visualizer against a real local audio file.
- Verify empty-library, unsupported-format, missing-file, and audio-device-unavailable states show a readable in-game message rather than failing silently.
- Keep the browser integration optional and separate; it must not be a dependency for native offline playback.

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
- [ ] Resolve the native offline playback blocker above, then verify MP3/OGG/WAV output and that no external media player is launched.
- [ ] Verify Recently Played and Listening Stats update after playback and persist after restart.
- [ ] Manual audio, optional online-browser bridge, and visual-effects smoke tests remain required.

CI success confirms compilation, automated tests, resource verification and packaging only. It does not confirm mouse interaction or audio output inside TLauncher. Native in-game playback remains unverified and is currently blocked by the user's report that the song list and audio do not work as expected.

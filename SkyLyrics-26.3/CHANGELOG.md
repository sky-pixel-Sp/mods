# Changelog

All notable SkyLyrics changes are recorded here.

## [0.10.0] — 2026-11-08

### Changed
- Music Center is now the first screen opened by the O key while inside a world or multiplayer server.
- Music Center and its in-game file browser do not pause the game.
- Added an in-game audio / LRC file browser with Home, Music, Downloads, Desktop, and parent-folder navigation.
- Removed the Online Music tab and all UI entry points that opened Spotify or YouTube.
- Disabled the browser media bridge at client startup; this edition plays only local files within Minecraft.
- Removed the native desktop file dialog from the import workflow.
- Refined the UI palette and corrected overlapping Now Playing and Offline Library information.
- Removed the browser extension from the distributable install package.

### Compatibility
- Minecraft Java 26.3
- Fabric Loader 0.20.1+
- Fabric API 0.165.2+26.3
- Java 25+

### Verification boundary
- GitHub Actions validates compilation, tests, resources, and packaging.
- Live audio output, file navigation on the user's laptop, and GUI interaction still require a manual client test.


## [0.9.3] — 2026-11-08

### Fixed
- Isolated local audio workers by playback-session generation so an obsolete worker cannot overwrite the active track's state.
- Prevented a finishing or interrupted worker from closing a newer session's `SourceDataLine`.
- Removed the synchronous worker join from the client-facing stop path to avoid waiting on codec I/O during Minecraft interaction.
- Rechecked pending seek requests after a paused worker wakes, so seeking while paused is not lost.

### Tests
- Added JUnit regression tests for session start, session replacement, invalidation, and stale-token rejection.
- GitHub Actions runs the Java 25 / Gradle 9.8.2 `clean test build` pipeline, browser-extension checks, final-JAR resource checks, and package assembly.

### Compatibility
- Minecraft Java 26.3
- Fabric Loader 0.20.1+
- Fabric API 0.165.2+26.3
- Java 25+

### Not covered by automated CI
- Audible playback and audio-device behavior in a live Minecraft client.
- TLauncher GUI interaction, manual seek/volume behavior, Elytra rendering, and Chrome/Edge browser-session behavior.

## [0.9.2]

- Added the NASA/JPL photographic sky gallery, bilingual UI, additional HUD artwork presentation, and packaging/license checks.
- Added or refined the Music Center, offline library, browser media integration, lyric word timing, and HUD widgets. See the README for the feature guide.


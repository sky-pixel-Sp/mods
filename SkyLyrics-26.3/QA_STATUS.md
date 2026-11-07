# QA STATUS — SkyLyrics 26.3 / 0.2.0

## Checks completed

- Core LRC parser, cue model, and player clock compile using the Java 21 compiler available in the build environment.
- Runtime smoke test passed: LRC metadata, multiple timestamps, fractional timestamps, local pause/seek clock, external playback clock, pause state, and manual sync offset.
- Integration Java sources compile-checked with minimal Gson/SLF4J stubs. This checks Java syntax and the local method structure, but is not a substitute for compilation against the full Minecraft/Fabric classpath.
- Browser extension scripts pass `node --check`.
- Content-script simulation passed for YouTube, YouTube Music, and Spotify Web Player metadata payloads.
- Loopback HTTP smoke test passed: `/health` returns 200, a metadata POST without a Chromium extension Origin is rejected with 403, and a valid Chromium extension Origin passes the origin check.
- Browser extension manifest and Fabric mod JSON parse successfully.
- Loopback server source only binds to `127.0.0.1:38173`; supported POST sources are allow-listed as `youtube`, `youtube_music`, and `spotify_web`.
- POST endpoint checks Chromium extension `Origin` (`chrome-extension://` + 32-character extension ID); ordinary website origins are rejected.
- Extension uses `text/plain` JSON payload to avoid a cross-origin JSON preflight request.

## Not yet verified

- Fabric Loom build against Minecraft 26.3 / Fabric Loader / Fabric API was not run. The environment only has Java 21; Minecraft 26.3 requires JDK 25. Gradle dependencies cannot be downloaded from this environment.
- The browser extension has not been loaded into Chrome/Edge here.
- No real media-session test has been performed with YouTube, YouTube Music, or Spotify Web Player on Windows.
- No in-game render test has been performed in TLauncher or on a multiplayer server.

## Known behavior / guardrails

- YouTube / YouTube Music: extension sends track metadata, duration, playback state, and browser player position. SkyLyrics attempts a matching synced-LRC lookup through LRCLIB and checks exact title/artist plus duration where available. If no synced result is found, a local `.lrc` file can be loaded instead.
- Spotify Web Player: Now Playing metadata is supported. Synchronized lyrics are intentionally disabled because Spotify's Developer Policy prohibits synchronizing sound recordings with visual media.
- No Spotify API, OAuth, account credentials, audio capture, download, or playback control is used.
- The browser extension sends only metadata to the mod's loopback endpoint. No endpoint is exposed to the LAN.
- Client-side only; no server-side mod/plugin is required.

## Release gate

Do not label this as a tested JAR release until all of the following pass on the target Windows PC:

1. JDK 25 `build.bat` completes `clean test build` with exit code 0.
2. A valid `skylyrics-0.2.0.jar` is produced.
3. Chrome/Edge extension loads without errors and connects to the mod endpoint.
4. `http://127.0.0.1:38173/health` returns `ok: true` while Minecraft is running.
5. YouTube and YouTube Music send metadata and the playhead advances; synced lyrics load only when a suitable LRCLIB result exists.
6. Spotify Web Player displays Now Playing metadata but does not activate synchronized lyrics.
7. Closing the music tab or stopping playback does not freeze or crash Minecraft.
8. Joining a multiplayer server does not require installing SkyLyrics server-side.

# QA STATUS — SkyLyrics 0.5.0 / Minecraft Java 26.3

## Automated checks included in this branch

- JUnit tests cover LRC parsing, lyric timing, preset catalog behavior, clean-install cinematic defaults, and migration from the 0.4 settings format.
- The Gradle verification task checks all 20 procedural sky post-effect definitions and the four shader stages.
- The GitHub Actions workflow is configured to use Temurin JDK 25 and Gradle 9.8.2, then run clean test build. The default-branch workflow now reads the JAR version from gradle.properties so feature branches are not incorrectly checked for the old 0.3.0 filename.
- The workflow checks the built JAR manifest, pinned custom fonts, font provider JSON, AlbumArtManager, font license notices, and browser-extension JavaScript/manifest.
- A built-in original-text preview is exposed through the client command /skylyrics demo and /skylyrics stop, and through the Studio overview.

## Changes made for the visual mismatch

- The default sky preset is Golden Sunset, matching the warm cinematic sky in the reference video.
- The sky effect is applied on the first client-world entry, rather than waiting for the user to open SkyLyrics Studio.
- Old 0.4 configurations with a 0.012–0.060 lyric scale are migrated once to a larger 0.115 default. A custom sky preset and an already larger custom scale are preserved.
- Word-by-word lyric reveal is enabled after migration.
- World lyrics use warm-white text with a restrained dark outline, no opaque background label, a small upward sky offset, and automatic line-width fitting.
- The Studio overview includes an offline preview control and a current-track status panel.

## Not yet verified

- The 0.5.0 GitHub Actions run must finish successfully before the JAR is treated as a build artifact.
- The mod has not yet been manually launched in the user's TLauncher installation after these changes.
- The in-game appearance of the new world-text scale, depth behavior, custom font rendering, and sky preset still needs a visual smoke test on the target GPU.
- Browser media integration still requires the unpacked Chrome/Edge extension. Spotify Web Player remains metadata/artwork only; synchronized Spotify lyrics are not enabled.

## Manual release gate

1. Confirm GitHub Actions completes clean test build with exit code 0.
2. Extract the uploaded skylyrics-0.5.0.jar and confirm Fabric Loader 0.20.1+ and Fabric API 0.165.2+26.3 are installed in the same 26.3 client profile.
3. Enter a single-player world. Golden Sunset should apply automatically without opening the O menu first.
4. Run /skylyrics demo. Large 3D lyrics should appear above the horizon, reveal word by word, and remain readable without a dark rectangular panel.
5. Run /skylyrics stop, open O → Styles, and verify Poppins/Montserrat and the other bundled font families still render.
6. Join a multiplayer server. SkyLyrics should remain client-only and must not require a server-side mod or resource pack.
7. If YouTube / YouTube Music is used, load the included browser extension and verify media metadata and the synced-LRC lookup. A missing LRCLIB match should be reported clearly rather than displaying fake lyrics.

Do not mark this branch as visually verified until the manual in-game check is completed.
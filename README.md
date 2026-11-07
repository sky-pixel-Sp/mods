# SkyLyrics — Minecraft Java 26.3

A client-side Fabric mod that displays synchronized lyrics in the Minecraft sky/HUD.

## Source code

The complete, readable Gradle project is stored in [`SkyLyrics-26.3/`](SkyLyrics-26.3/). The browser connector is in `SkyLyrics-26.3/browser-extension/`.

The previous File Converter HTML app is preserved at the repository root. It is marked as a legacy/vendored file in `.gitattributes` so it does not dominate the repository language statistics.

## GitHub Actions build

1. Open the repository's **Actions** tab.
2. Select **Build SkyLyrics for Minecraft 26.3**.
3. Open the latest run for branch `skylyrics-26.3`.
4. Confirm that the build job succeeds.
5. Download the **SkyLyrics-Minecraft-26.3** artifact from the run.
6. Extract the artifact ZIP. It contains `skylyrics-0.2.0.jar`.

The workflow verifies the source archive SHA-256, installs JDK 25 and Gradle 9.8.2, expands the source into normal `.java`, `.js`, and project files in the branch, runs `clean test build`, verifies the JAR contains `fabric.mod.json`, and uploads the JAR.

## Music integrations

- **YouTube and YouTube Music:** the browser extension sends playback metadata and position; SkyLyrics attempts a synced-LRC lookup.
- **Spotify Web Player:** Now Playing metadata only. Spotify synchronized lyrics are not enabled.
- **Local LRC:** load a local `.lrc` file as a fallback.

## Install in TLauncher

Use a Minecraft Java 26.3 Fabric profile. Place the SkyLyrics JAR and the matching Fabric API JAR in that profile's `mods` folder. Load the included `browser-extension` folder as an unpacked extension in Chrome or Edge to connect browser music metadata.

SkyLyrics is client-side; the multiplayer server does not need it installed.

## Release status

The source and CI workflow are prepared. Do not treat the JAR as a tested release until a workflow run succeeds and the artifact has been tested in Minecraft 26.3.

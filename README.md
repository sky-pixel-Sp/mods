# SkyLyrics — Minecraft Java 26.3

Client-side Fabric mod that displays synchronized lyrics in the Minecraft sky/HUD.

## GitHub Actions build

The `skylyrics-26.3` branch contains the SkyLyrics source archive and its automated build workflow.

1. Open the repository's **Actions** tab.
2. Select **Build SkyLyrics for Minecraft 26.3**.
3. Open the latest run for branch `skylyrics-26.3`.
4. Wait for the job to finish successfully.
5. Download the **SkyLyrics-Minecraft-26.3** artifact from the run's Artifacts section.
6. Extract the artifact ZIP. It contains `skylyrics-0.2.0.jar`.

The workflow verifies the source archive SHA-256, installs JDK 25 and Gradle 9.8.2, runs `clean test build`, verifies that the built JAR contains `fabric.mod.json`, and uploads the JAR.

## Music integrations

- **YouTube and YouTube Music:** the browser extension sends playback metadata and position; SkyLyrics attempts a synced-LRC lookup.
- **Spotify Web Player:** Now Playing metadata only. Spotify synchronized lyrics are not enabled.
- **Local LRC:** load a local `.lrc` file as a fallback.

## Install in TLauncher

Use a Minecraft Java 26.3 Fabric profile. Place the SkyLyrics JAR and the matching Fabric API JAR in that profile's `mods` folder. Load the included `browser-extension` folder as an unpacked extension in Chrome or Edge to connect browser music metadata.

SkyLyrics is client-side; the multiplayer server does not need it installed.

## Release status

The Actions workflow is configured to compile, test, and package SkyLyrics. Do not treat the JAR as a tested release until a workflow run succeeds and the artifact has been tested in Minecraft 26.3.

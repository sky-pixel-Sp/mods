# SkyLyrics — Minecraft Java 26.3

A client-side Fabric mod that displays synchronized lyrics in the Minecraft sky/HUD.

## Source code

The complete, readable Gradle project is in [`SkyLyrics-26.3/`](SkyLyrics-26.3/), including its Java source, browser connector, sample LRC, resources, and tests.

The old File Converter HTML app remains at the repository root. It is marked as a legacy/vendored file in `.gitattributes` so it does not dominate GitHub language statistics.

## Automated build

GitHub Actions builds this project on every relevant push to `main`.

1. Open the repository's **Actions** tab.
2. Select **Build SkyLyrics for Minecraft 26.3**.
3. Open a successful run.
4. Download the **SkyLyrics-Minecraft-26.3** artifact.
5. Extract the ZIP. It contains `skylyrics-0.2.0.jar`.

The workflow installs Temurin JDK 25, explicitly selects it for Gradle, configures Gradle 9.8.2, runs `clean test build`, verifies the JAR contains `fabric.mod.json`, and uploads the artifact.

## Music integrations

- **YouTube and YouTube Music:** the browser extension sends playback metadata and position; SkyLyrics attempts a synced-LRC lookup.
- **Spotify Web Player:** Now Playing metadata only. Spotify synchronized lyrics are not enabled.
- **Local LRC:** load a local `.lrc` file as a fallback.

## Install in TLauncher

Use a Minecraft Java 26.3 Fabric profile. Place the SkyLyrics JAR and the matching Fabric API JAR in that profile's `mods` folder. Load the included `browser-extension` folder as an unpacked extension in Chrome or Edge to connect browser music metadata.

SkyLyrics is client-side; the multiplayer server does not need it installed.

## Release status

The source project is maintained in this repository. Treat the JAR as a tested build only after the corresponding GitHub Actions run succeeds and the mod has been launched in Minecraft 26.3.

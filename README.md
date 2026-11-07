# SkyLyrics — Minecraft Java 26.3

**SkyLyrics 0.5.0 — Cinematic Edition** is a client-side Fabric mod that renders large synchronized lyrics as live 3D text in the Minecraft sky, with procedural sky presets and a configurable studio menu.

## Project

The complete Gradle project is in the SkyLyrics-26.3/ directory. It includes Java source, bundled fonts and OFL notices, browser media connector, shader resources, automated tests, and build scripts.

## Build on GitHub

The workflow **Build SkyLyrics for Minecraft 26.3** uses JDK 25 and Gradle 9.8.2. It runs unit tests, compiles the Fabric mod, checks the JAR contents and bundled font resources, validates the browser connector, and uploads the build artifact.

1. Open the repository's **Actions** tab.
2. Select **Build SkyLyrics for Minecraft 26.3**.
3. Open a successful run on the skylyrics-0.5-cinematic-overhaul branch.
4. Download the artifact named **SkyLyrics-Minecraft-26.3**.
5. Extract the ZIP. The mod file is skylyrics-0.5.0.jar.

Do not treat a JAR as a verified release until its workflow run succeeds. CI does not replace an in-game visual test on the target Windows PC.

## Install

Use Minecraft Java 26.3 with Fabric Loader 0.20.1+ and Fabric API 0.165.2+26.3. Put the SkyLyrics JAR and Fabric API JAR in the same client mods folder.

SkyLyrics is client-side. The multiplayer server does not need the mod installed.

## Quick preview

Enter a world and run:

    /skylyrics demo

The built-in preview uses original sample text and does not need a browser extension, song file, account, or server. Run /skylyrics stop to end it.

See SkyLyrics-26.3/README.md for all settings, music integration, requirements, and troubleshooting.
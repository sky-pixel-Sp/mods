# SkyLyrics 0.5.0 — Cinematic Edition

A client-side Fabric mod for Minecraft Java 26.3. SkyLyrics turns music into large, timed 3D typography in the Minecraft sky, with procedural sky effects and a configurable studio menu.

## What changed in 0.5.0

- **Cinematic 3D lyrics:** larger world-space type, word-by-word reveal by default, soft dark outline, and no opaque box behind the lyric.
- **Automatic line fitting:** long lines scale down to stay inside a comfortable field of view instead of stretching across the whole world.
- **Golden Sunset is the new default sky:** the sky effect is applied automatically when a world is entered; it no longer waits for the settings screen to be opened.
- **Legacy settings migration:** existing 0.4 settings are upgraded once, replacing the old tiny 3D text scale with a cinematic default while preserving a custom sky preset and a deliberately larger text size.
- **Offline preview:** use /skylyrics demo to play an original built-in lyric preview without a music account, browser connector, server, or resource pack. Use /skylyrics stop to stop it.
- **Studio overview:** a dedicated preview control, live lyric status, expanded workspace, and clearer active-tab indicator.
- **Bundled typography and mini-player:** eight bundled OFL font families and allow-listed album artwork remain available from 0.4.0.

## Visual direction

The visual target is a cinematic sunset scene with warm sky gradients, very large warm-white lyrics floating above the horizon, subtle shadow/outline, and a compact now-playing pill. Sky backgrounds are generated procedurally by GLSL; there are no baked landscape photographs used as the in-game sky. Lyrics are rendered as live 3D text in the world.

The video reference is used as a visual guide, not as an asset to copy. Text, animation, and sky effects are drawn at runtime.

## Features

- **Touch-friendly Studio:** all major controls are clickable in-game; the new Player tab opens the local lyrics folder, loads LRC tracks, and controls the lyric timeline without chat commands.
- **Rebindable controls:** O (Studio), H (lyrics visibility), and K (timeline play/pause) are registered as normal Minecraft key mappings and can be changed in Options → Controls → SkyLyrics.

- 20 procedural sky presets: Aurora, Galaxy, Nebula Storm, Nebula, Sunset, Golden Sunset, Pink Sunset, Saturn, Meteors, Star Trails, Comet, Hyperspace, Blood Moon, Solar Flare, Dreamscape, Prism, Synthwave, Black Hole, Thunderstorm, and Ocean.
- Eight font families: Poppins, Montserrat, Kanit, Fredoka, Unbounded, Bebas Neue, Pacifico, and Caveat.
- Three-dimensional world lyrics or an on-screen HUD.
- Word reveal, camera follow/fixed placement, automatic line fitting, text glow/outline, size and distance controls.
- Local LRC library and automatic synchronized lyric lookup for YouTube / YouTube Music.
- Now-playing metadata and artwork for supported browser media sessions.
- Client-only operation; a multiplayer server does not need SkyLyrics installed.

## Requirements

- Minecraft Java 26.3.
- Fabric Loader 0.20.1 or newer.
- Fabric API 0.165.2+26.3.
- Java Runtime 25 or newer.
- Windows 10/11 for browser media integration.
- Chrome or Edge for YouTube / YouTube Music metadata integration.

## Install in TLauncher

1. Install Fabric Loader for Minecraft 26.3 and start the Fabric profile once.
2. Close Minecraft.
3. Put skylyrics-0.5.0.jar and the matching Fabric API JAR in the same mods folder used by that Fabric profile.
4. Start the Fabric 26.3 profile again.
5. Press O to open SkyLyrics Studio. The selected sky is also applied automatically after entering a world.
6. Open the Player tab to load local LRC files and control the lyric timeline with on-screen buttons. The cinematic preview is available from Home.

No SkyLyrics JAR or resource pack is installed on the multiplayer server. This mod runs on the player's client.

## Controls

| Key / command | Function |
|---|---|
| O | Open SkyLyrics Studio |
| H | Show / hide lyric display |
| K | Play / pause a local LRC or demo track |
| /skylyrics demo | Start the offline cinematic preview |
| /skylyrics stop | Stop the preview |

## Music integration

**Important playback distinction:** the current Player tab controls the synchronized lyric timeline, not an audio decoder. Offline `.lrc` files work without internet, but the current build does not yet decode/play downloaded MP3/OGG/WAV audio inside Minecraft. YouTube / YouTube Music audio continues playing in the browser; the extension supplies metadata and position for lyric synchronization. Spotify currently supplies metadata/artwork only. These limits are shown in the in-game Player/Information screens rather than being presented as working audio playback.


For YouTube and YouTube Music, load the included browser-extension folder as an unpacked extension in Chrome or Edge. The extension sends playback metadata and position to the local SkyLyrics bridge; it does not capture audio.

Spotify Web Player provides Now Playing metadata and artwork only. Synchronized Spotify lyrics are not enabled. A local .lrc file can be placed in config/skylyrics/lyrics/ and loaded through the Lyrics screen.

## Build and QA

GitHub Actions uses JDK 25 and Gradle 9.8.2. The workflow runs unit tests and a full Fabric Loom build, validates the JAR metadata, bundled fonts, font licenses, and browser-extension scripts, then publishes the JAR as a workflow artifact.

A green CI build confirms source compilation and automated checks. It does not replace the final visual smoke test in TLauncher on the target GPU. The cinematic preview is included to make that manual check repeatable and independent of a real song.

## Licensing and privacy

SkyLyrics code is MIT. Bundled font families are distributed under their respective SIL Open Font License notices included in the JAR. The browser connector sends only media metadata and artwork URLs to localhost. Audio is not recorded, downloaded, or uploaded.
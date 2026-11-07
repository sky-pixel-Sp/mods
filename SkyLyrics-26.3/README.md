# SkyLyrics 0.6.0 — Music Center

SkyLyrics is a client-side Fabric mod for Minecraft Java 26.3. It combines an in-game clickable Music Center, local offline audio playback, browser-connected online music controls, synchronized lyrics, and optional cinematic sky effects.

## Music Center

Open the interface with **O**. The entire player is operated through clickable Minecraft UI buttons; chat commands are not required.

- **Music Center:** play, pause, stop, previous/next, seek, and volume controls.
- **Offline Library:** import and play local music files, browse the saved library, import matching LRC files, and open the library folder.
- **History & Stats:** locally stored play-session count, unique recent tracks, and the latest 100 playback sessions; history can be cleared from the UI.
- **Online Music:** open YouTube Music or Spotify Web in the browser and control the active browser player from Minecraft.
- **Lyrics Studio:** floating 3D lyrics or HUD lyrics, LRC import, word reveal, font and style controls.
- **Keybinds:** all SkyLyrics shortcuts appear in Minecraft's Controls screen and can be rebound there.
- **Skies / Performance:** choose a procedural sky or disable the sky effect entirely.

## Offline playback

1. Download or obtain your music file from a source that permits you to keep and play it locally.
2. In Minecraft press **O → Music Center → Import Song**, or open **Offline Library → Import Audio File**.
3. Select an audio file. SkyLyrics copies it into `config/skylyrics/music/` so the library can find it again later.
4. Use the on-screen Play/Pause, Previous, Next, Seek and Volume buttons. Offline playback does not require Wi-Fi or mobile data.

Supported formats: **MP3, OGG, WAV, AIFF and AU**. MP3 and OGG decoding is supplied by bundled Java Sound codec libraries. WAV/AIFF/AU use the Java runtime audio providers.

The compact in-game waveform reacts to measured PCM amplitude for offline tracks. Browser playback shows a lightweight transport animation only; SkyLyrics does not capture browser audio.

To show synchronized lyrics offline, import a matching `.lrc` file into the Lyrics Library. The audio and lyric files must share the same base filename, for example `Golden Hour.mp3` and `Golden Hour.lrc`. You can also import the LRC from the Music Center.

Music library directory: `config/skylyrics/music/`.
Lyrics directory: `config/skylyrics/lyrics/`.
Listening history: `config/skylyrics/history.json` (metadata only; stored locally and removable from History & Stats).

## Online playback

Online mode uses the official website player in Chrome or Edge. Internet access is required, and you must sign in to the music provider yourself if it requires an account.

1. Press **O → Online Music**.
2. Click **Open YouTube Music** or **Open Spotify Web**. The provider opens in your browser; start playing music there.
3. Install the included `browser-extension` folder as an unpacked extension in Chrome or Edge and keep the provider tab open.
4. When the extension is connected, SkyLyrics shows the current track and exposes browser playback controls in the Minecraft UI.

The browser streams the music directly. SkyLyrics does not capture, download, or relay audio bytes from the online provider. The local extension only sends supported track metadata and receives a small allow-listed set of user playback commands over `127.0.0.1:38173`. Track/session metadata is recorded in the local History & Stats page; no audio is captured or uploaded by SkyLyrics.

YouTube / YouTube Music can request synchronized LRC lyrics when a matching result is available. If none is found, import an LRC file manually. Spotify Web displays Now Playing metadata and artwork; synchronized Spotify lyrics are intentionally disabled.

## Controls and keybinds

| Default key | Action |
|---|---|
| O | Open SkyLyrics Music Center |
| H | Show / hide lyric display |
| K | Play / pause current track |
| N | Next local library track |
| B | Previous local library track |

Every key is configurable in **Minecraft → Options → Controls → Key Binds → SkyLyrics**. The UI buttons work with the standard Minecraft screen pointer; no chat command is needed.

## Listening history and visualizer

History & Stats records a local play session when a local track is selected or a supported browser track is observed playing. It stores only title, artist, source, and timestamp in `config/skylyrics/history.json`; it does not store audio, account identifiers, or authentication tokens. The most recent 100 sessions are kept, while the total session count is retained. The history can be cleared from the in-game page.

For local audio, the mini-player waveform uses a short-window RMS measurement from the PCM stream. Online browser playback deliberately does not expose captured audio samples; its bars are only a playback-state animation.

## Shader is optional

The Music Center, offline audio engine, online browser bridge, playlists/library, and lyric synchronization do not depend on the sky shader. Turn off **Sky Engine** in the UI or use the Performance tab; music and lyrics continue to work.

## Requirements

- Minecraft Java 26.3.
- Fabric Loader 0.20.1 or newer.
- Fabric API 0.165.2+26.3.
- Java Runtime 25 or newer.
- Windows / desktop environment with Java Sound output for local music. The file picker uses the operating system's native dialog.
- Chrome or Edge for online browser integration.

## Install in TLauncher

1. Start the Minecraft 26.3 Fabric profile once, then close Minecraft.
2. Put `skylyrics-0.6.0.jar` and the matching Fabric API JAR in the `mods` folder used by that Fabric profile.
3. Start TLauncher and select the Fabric 26.3 profile.
4. Enter a world and press **O** to open SkyLyrics Music Center.

SkyLyrics is client-side; a multiplayer server does not need this mod installed. No server resource pack is required.

## Build and verification

GitHub Actions uses JDK 25 and Gradle 9.8.2. The pipeline runs unit tests, verifies sky shaders and fonts, compiles the Minecraft 26.3 Fabric client, validates browser-extension JavaScript, verifies audio/player classes, and uploads the mod JAR.

A green CI result confirms compilation, unit tests, and packaging. It does not replace the final manual audio test in TLauncher. Local MP3/OGG output and online browser controls must be smoke-tested on the target Windows PC before calling this a fully verified release.

## Licenses

SkyLyrics source code is MIT. Bundled fonts carry their SIL Open Font License notices. MP3SPI and VorbisSPI codec providers are LGPL-2.1; their attribution and upstream links are included in `META-INF/licenses/skylyrics-audio/NOTICE.txt`. Browser extension code is local and does not send audio to a remote server.
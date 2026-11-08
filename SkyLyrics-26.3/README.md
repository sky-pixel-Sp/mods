# SkyLyrics 0.10.0 — by Sky

## 0.10.0 — fully in-game local music

SkyLyrics opens directly into **Music Center** when you press **O inside a world or multiplayer server**. The screen does not pause the world. Music is selected with SkyLyrics' own in-game file browser, copied into the local library, and played by the mod's Java Sound engine. No Spotify, YouTube, browser tab, native desktop file dialog, or external media player is used by this edition.

The 0.9.3 audio-session safety improvements are retained: every audio worker has its own session identity, old workers cannot close a newer track's audio line, and stopping a track does not wait on codec I/O on the Minecraft client thread.

The CI build and packaged-JAR checks do not replace a manual audio-output test in a real Minecraft 26.3 client. See [QA_STATUS.md](QA_STATUS.md) for the exact verification boundary.

SkyLyrics by **Sky** is a client-side Fabric mod for Minecraft Java 26.3. It provides a fully in-game local music player, an in-game file browser, synchronized lyrics, a customizable HUD, and optional cinematic sky effects.

## Language support

The in-game interface defaults to **English**. Open **Settings → Language** to switch to **Bahasa Indonesia**; your selection is saved locally. The English documentation in this README is the primary guide. See [CHANGELOG.md](CHANGELOG.md) for version history, [QA_STATUS.md](QA_STATUS.md) for verified checks and limitations, and [PANDUAN-ID.md](PANDUAN-ID.md) for the Indonesian installation guide.

## Music Center

The UI language defaults to English and can be switched to Indonesian from the Settings tab. Press **O while playing in a world or server** to open Music Center; the world continues running behind it.

Open the interface with **O**. The entire player is operated through clickable Minecraft UI buttons; chat commands are not required.

- **Music Center:** play, pause, stop, previous/next, seek, and volume controls. The player opens inside the active world or server and does not pause gameplay.
- **Offline Library:** browse and play imported local music files, import matching LRC files, and keep your library inside `config/skylyrics/music/`.
- **History & Stats:** locally stored play-session count, unique recent tracks, and the latest 100 playback sessions; history can be cleared from the UI.
- **Artwork Glow:** the Now Playing card samples a color from the current cover art and uses it for a restrained pulsing glow when GLOW is enabled.
- **Lyrics Studio:** floating 3D lyrics or HUD lyrics, standard LRC import, word-by-word reveal, font and style controls.
- **Elytra Lyrics Mode:** while gliding, the 3D lyrics move farther ahead and remain camera-facing; the option can be disabled in HUD settings.
- **Now Playing styles:** Bordered, Borderless, and Compact presentations, selectable from Styles.
- **Manual sync offset:** shift lyric timing by 250 ms from the Lyrics page and reset it without editing files. The offset applies to the current loaded track.
- **Word-synced karaoke:** reads LRCLIB-style inline word timestamps such as `<00:12.42>` and highlights each word at its actual timestamp. Ordinary line-timed LRC files continue to work.
- **HUD Layout Studio:** five independent cards (Now Playing, Lyrics, PCM Visualizer, Recently Played and Listening Stats) can be moved separately. Positions persist across restarts and resolution changes; double-click resets one card and right-click cancels a drag.
- **Live PCM Visualizer:** the offline player measures decoded PCM amplitude and drives a dedicated multi-bar HUD card. The visualizer is driven only by local audio.
- **Recently Played HUD:** shows the latest three locally recorded listening sessions, including an elapsed-time label.
- **Listening Stats HUD:** exposes persistent play count and recent unique-track count from the local history store. Optional cards can be enabled/disabled in the HUD Layout Studio.
- **Keybinds:** change the five SkyLyrics shortcuts directly in the in-game KEYBINDS page. Click a row, press any keyboard key or mouse button, or press ESC to cancel. Restore Defaults is one click away. The same mappings remain visible in Minecraft's Controls screen.
- **Skies / Performance:** choose a procedural sky or disable the sky effect entirely.


## Quick start — in-game

1. Launch Minecraft Java **26.3 + Fabric**, enter a single-player world or multiplayer server, and press **O**. Music Center opens inside Minecraft without pausing the world.
2. Choose **Import Song**. SkyLyrics opens its own in-game file browser; browse to `Music`, `Downloads`, or another folder and click an audio file.
3. SkyLyrics copies the song into `config/skylyrics/music/` and plays it through the in-game audio engine. No web service, browser, Spotify, YouTube, or separate media player is launched.
4. Choose **Import / Match LRC** in Music Center and select the matching `.lrc` file. You can also import it from the Offline Library.
5. In **Lyrics**, choose **Floating World Text** for 3D lyrics or **Screen HUD** for a 2D overlay. Use **Keybinds** to change shortcuts.

## Offline playback

1. Download or obtain your music file from a source that permits you to keep and play it locally.
2. In Minecraft press **O → Music Center → Import Song**, or open **Offline Library → Import Audio File**.
3. Select an audio file. SkyLyrics copies it into `config/skylyrics/music/` so the library can find it again later.
4. Use the on-screen Play/Pause, Previous, Next, Seek and Volume buttons. Offline playback does not require Wi-Fi or mobile data.

Supported formats: **MP3, OGG, WAV, AIFF and AU**. MP3 and OGG decoding is supplied by bundled Java Sound codec libraries. WAV/AIFF/AU use the Java runtime audio providers.

The in-game waveform reacts to measured PCM amplitude from local tracks. Audio playback, controls, and lyric timing stay within the Minecraft client.

To show synchronized lyrics offline, import a matching `.lrc` file into the Lyrics Library. The audio and lyric files must share the same base filename, for example `Golden Hour.mp3` and `Golden Hour.lrc`. You can also import the LRC from the Music Center.

Music library directory: `config/skylyrics/music/`.
Lyrics directory: `config/skylyrics/lyrics/`.
Listening history: `config/skylyrics/history.json` (metadata only; stored locally and removable from History & Stats).

## Controls and keybinds

| Default key | Action |
|---|---|
| O | Open SkyLyrics Music Center |
| H | Show / hide lyric display |
| K | Play / pause current track |
| N | Next local library track |
| B | Previous local library track |

Every key is configurable in **O → KEYBINDS** or **Minecraft → Options → Controls → Key Binds → SkyLyrics**. In the KEYBINDS page, click a control row and then press the keyboard key or mouse button to assign it. Press ESC to cancel capture or select **Restore Default Keys** to restore O / H / K / N / B. No chat command is needed.

## HUD layout editor

Open **O → HUD → Open HUD Layout Editor**. Drag the **Now Playing** card and the **Lyrics HUD** panel independently. The editor saves normalized screen coordinates, so the layout is retained when the game resolution changes. Double-click a panel to reset only that panel; click **Reset All** to restore both defaults. Right-click while dragging to cancel and restore the position from before that drag.

The editor changes only client-side HUD placement. It does not alter the world, player position, server state, or resource packs.

The HUD Layout Studio now includes VIS, RECENT, and STATS visibility switches. Every card can be dragged, independently reset by double-click, or restored to its previous position with right-click. New widget positions and visibility choices are stored in configuration schema version 4; existing settings migrate automatically. SkyLyrics 0.9.2 advances the settings schema to version 5 for the Elytra toggle and Now Playing style.

## Word-synchronized lyrics

SkyLyrics accepts ordinary LRC line timestamps and LRCLIB-style word timestamps embedded inside a line, for example:

\`\`\`lrc
[00:12.00]<00:12.00>Good <00:12.55>morning <00:13.20>sky
\`\`\`

When word timestamps are present and **Word-by-word reveal** is enabled, the screen HUD highlights the active word and the floating 3D lyrics reveal words in time. For LRC files without inline word timestamps, SkyLyrics falls back to its existing line-progress reveal behavior.

A detailed independent architecture review of the supplied VerseFall 1.1.0 archive, its media/lyrics/HUD subsystems, compatibility limits, and the features chosen for SkyLyrics is in [`VERSEFALL_TECHNICAL_REVIEW.md`](VERSEFALL_TECHNICAL_REVIEW.md). The 0.9.2 presentation additions were also informed by the feature description of [Lyrics by QQID](https://www.curseforge.com/minecraft/mc-mods/lyrics); SkyLyrics retains its own `skylyrics` mod ID, package namespace, and branding. No third-party JAR is bundled.


## Listening history, artwork glow and visualizer

History & Stats records each local play session. It stores only title, artist, source, and timestamp in `config/skylyrics/history.json`; it does not store audio or account identifiers. The most recent 100 sessions are kept, while the total session count is retained. The history can be cleared from the in-game page.

The Now Playing presentation uses a restrained accent derived from SkyLyrics' selected visual style. No third-party cover artwork is required for local playback.

The mini-player waveform uses a short-window RMS measurement from the local PCM stream.

## Shader is optional

The Music Center, offline audio engine, library, and lyric synchronization do not depend on the sky shader. Turn off **Sky Engine** in the UI or use the Performance tab; music and lyrics continue to work.

## Requirements

- Minecraft Java 26.3.
- Fabric Loader 0.20.1 or newer.
- Fabric API 0.165.2+26.3.
- Java Runtime 25 or newer.
- Windows / desktop environment with Java Sound output for local music. The file picker uses the operating system's native dialog.

## Install in TLauncher

1. Start the Minecraft 26.3 Fabric profile once, then close Minecraft.
2. Put `skylyrics-0.10.0.jar` and the matching Fabric API JAR in the `mods` folder used by that Fabric profile.
3. Start TLauncher and select the Fabric 26.3 profile.
4. Enter a world and press **O** to open SkyLyrics Music Center.

SkyLyrics is client-side; a multiplayer server does not need this mod installed. No server resource pack is required.

## Build and verification

GitHub Actions uses JDK 25 and Gradle 9.8.2. The pipeline runs unit tests, verifies sky shaders and fonts, compiles the Minecraft 26.3 Fabric client, verifies audio/player classes, and packages the Minecraft-only installation files.

SkyLyrics 0.10.0 is checked by GitHub Actions with JDK 25 and Gradle 9.8.2. The pipeline runs `clean test build`, verifies runtime resources and nested audio codecs in the final JAR, and assembles the Minecraft-only install package. The run number, artifact identifier, package size, and SHA-256 are recorded in [QA_STATUS.md](QA_STATUS.md). CI does not verify audible output on a physical audio device, in-game file navigation, or mouse interactions inside TLauncher; these still require a manual Minecraft 26.3 client test.

## Package contents and asset verification

The Skies selector displays genuine NASA/JPL astronomy photographs as previews for the procedural sky presets. The build downloads the full-resolution source JPGs, validates them, and generates optimized 1600×900 preview textures. The source masters stay in the build workspace; the **eight preview textures actually used by the in-game selector** are bundled in the runtime JAR. Source page URLs, per-image credit lines, and the JPL image-use policy are included in `META-INF/licenses/skylyrics-artwork/NOTICE.txt`.

The distributable JAR carries the Sky icon, eight embedded font files with their OFL notices, 20 sky post-effect presets, four shader files, five nested audio codec libraries, and eight 1600×900 NASA/JPL preview textures. The build verifies all of those entries inside the **final JAR**, not just in the source folders. The current 0.10.0 artifact's exact size and SHA-256 are recorded in [QA_STATUS.md](QA_STATUS.md). The build also enforces a hard **5,000,000-byte minimum**; no padding, duplicate images, empty files, or dummy assets are used.

## Licenses

SkyLyrics source code is MIT. Bundled fonts carry their SIL Open Font License notices. MP3SPI and VorbisSPI codec providers are LGPL-2.1; their attribution and upstream links are included in `META-INF/licenses/skylyrics-audio/NOTICE.txt`. The 0.10.0 install package does not include or activate the former browser extension.
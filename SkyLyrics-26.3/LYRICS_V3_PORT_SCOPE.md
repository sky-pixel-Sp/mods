# Lyrics v3 reference and SkyLyrics 26.3 port scope

## Reference identity
- CurseForge project: [Lyrics by QQID](https://www.curseforge.com/minecraft/mc-mods/lyrics)
- User-supplied file 9041952: `lyricsplus-3.0.0-1.20.1.jar` (Minecraft 1.20.1 / Fabric).
- Closest newer reference release found: file 9041918, `lyricsplus-3.0.0-26.2.jar` (Minecraft 26.2 / Fabric).
- CurseForge identifies the project license as MIT. The public file listing reviewed for this port does not list a 26.3 release.

## Important behavioral distinction
The reference mod's public description says the user plays music on their PC and the mod detects that media session, then displays title, cover, progress, and lyrics in Minecraft. That is not native in-game audio playback. SkyLyrics keeps its own local audio engine and Offline Library as the native playback path; no Spotify, browser, VLC, or other player is required for local files.

## Feature mapping
| Reference feature | SkyLyrics status / planned adaptation |
|---|---|
| Synchronized lyric HUD and floating 3D lyrics | Already implemented in SkyLyrics |
| LRCLIB timed lyrics | Existing lookup is retained; manual LRC import remains available |
| Album-art-accented Now Playing | Existing cover sampling/glow retained |
| Bordered / Borderless / Compact Now Playing | Added in SkyLyrics 0.9.0 |
| Elytra lyrics positioned ahead | Added in SkyLyrics 0.9.0 |
| Position, style, lines, sync offset controls | Position/style/lines already present; offset controls added in 0.9.0 |
| External PC media-session detection | Not required for native local playback; optional future capability only if explicitly requested |

## Branding and compatibility
The mod remains **SkyLyrics by Sky**. Fabric mod ID and namespace remain `skylyrics`; this is not a renamed copy of the CurseForge mod. Minecraft target remains Java 26.3, Fabric, Java 25.

No third-party binary or asset is bundled. The 0.9.0 additions are implemented against SkyLyrics' existing source tree, using the public feature description as a compatibility/design reference.

## Validation boundary
Build and automated tests are not a substitute for an in-game test. Elytra rendering, style layout, sync offset, native audio device output, and Offline Library listing still require a manual TLauncher Minecraft 26.3 smoke test.

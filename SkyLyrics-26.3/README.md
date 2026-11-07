# SkyLyrics — Minecraft Java 26.3

**SkyLyrics** menampilkan lirik bertimestamp sebagai overlay sinematik di layar/langit Minecraft. Versi sumber 0.2.0 menambahkan koneksi dari YouTube, YouTube Music, dan Spotify Web Player melalui ekstensi Chrome/Edge.

> **Status artefak:** paket ini berisi source code mod dan browser extension. JAR belum dikompilasi di lingkungan penyusunan ini karena runtime lokal hanya Java 21 dan tidak tersedia akses untuk mengambil dependensi Gradle. Build final perlu dilakukan di Windows dengan JDK 25. Jangan menganggap source ZIP ini sebagai JAR siap pasang.

## Integrasi musik 0.2.0

### YouTube dan YouTube Music — metadata + sinkron lirik otomatis

- Ekstensi Chrome/Edge mendukung halaman pemutaran YouTube (`www.youtube.com/watch…`) dan YouTube Music (`music.youtube.com`).
- Ekstensi membaca metadata serta posisi dan durasi pemutar video/audio di halaman tersebut.
- Ekstensi mengirim metadata ke endpoint lokal yang dibuka SkyLyrics saat Minecraft berjalan.
- SkyLyrics mencari lirik bertimestamp yang cocok di LRCLIB menggunakan judul, artis/channel, dan durasi lagu.
- Jika tidak ada hasil yang cocok atau synced lyrics tidak tersedia, SkyLyrics tetap menampilkan Now Playing dan mendukung file `.lrc` lokal.

### Spotify — Now Playing / metadata

- Spotify Web Player pada `open.spotify.com` dikenali oleh browser extension.
- Judul, artis, album, dan status play/pause dapat ditampilkan di HUD Minecraft.
- **Sinkronisasi lirik untuk Spotify sengaja dinonaktifkan.** Kebijakan Spotify Developer melarang sinkronisasi sound recording dengan media visual. SkyLyrics tidak memakai Spotify API, tidak meminta password/API key, tidak memutar atau mengunduh audio, dan tidak merekam suara.
- Dukungan yang diuji secara desain adalah Spotify Web Player melalui Chrome/Edge. Aplikasi Spotify desktop tidak dijamin terdeteksi oleh konektor ini.

### Privasi dan jaringan

- SkyLyrics membuka server HTTP lokal hanya pada `127.0.0.1:38173`, bukan LAN atau internet.
- Browser extension mengirim hanya sumber, judul, artis, album, status play/pause, serta timing browser yang relevan.
- Tidak ada audio yang direkam, diunggah, atau diekstrak.
- Untuk pencarian lirik YouTube/YouTube Music, mod mengirim judul, artis/channel, album bila tersedia, dan durasi ke LRCLIB. LRCLIB adalah layanan pihak ketiga; hasil tidak dijamin tersedia atau selalu tepat.
- Endpoint lokal hanya menerima metadata dari sumber `youtube`, `youtube_music`, dan `spotify_web`, memeriksa origin ekstensi Chromium, serta tidak memiliki perintah untuk mengendalikan pemutar.

## Arsitektur

```text
YouTube video ─────┐
YouTube Music tab ─┼─ Chrome/Edge extension ─┐
Spotify Web tab ───┘                         │
                                             ▼
                                  SkyLyrics local endpoint
                                     127.0.0.1:38173
                                             │
                                             ▼
                                       Fabric mod
                                             │
                          YouTube / YouTube Music: LRCLIB lookup
                                             │
                                             ▼
                                    Minecraft lyric HUD
```

SkyLyrics adalah **client-side mod**. Untuk menampilkan overlay di komputer sendiri, cukup pasang mod pada klien TLauncher dan ekstensi browser. Server multiplayer tidak perlu memasang SkyLyrics dan tidak menerima metadata musik atau file lirik. Pemain lain tidak akan melihat overlay milikmu kecuali mereka memasang mod sendiri.

## Fitur utama

- Lirik LRC dengan multi timestamp, offset, dan pecahan detik.
- HUD dengan baris sebelumnya/aktif/berikutnya, judul, artis, dan progress.
- 5 palet visual: Golden Hour, Aurora, Violet Dream, Ocean, Monochrome.
- 5 gaya teks: Aurora Glow, Cinema Soft, Clean Minimal, Dream Glow, Classic Shadow.
- Panel Player, Library, Skies, Styles, dan Settings.
- Sinkronisasi timer eksternal dari YouTube dan YouTube Music.
- Now Playing untuk YouTube, YouTube Music, dan Spotify Web Player.
- Penyesuaian offset sinkronisasi.
- Library file `.lrc` lokal sebagai fallback.

## Persyaratan

- Minecraft Java **26.3**.
- JDK **25 64-bit** untuk build mod.
- Fabric Loader **0.20.1 atau lebih baru**.
- Fabric API **0.165.2+26.3**.
- TLauncher yang mendukung profil Fabric 26.3.
- Google Chrome atau Microsoft Edge untuk koneksi YouTube, YouTube Music, dan Spotify Web Player.
- Koneksi internet saat pencarian lirik otomatis dan saat build pertama.

## Build JAR mod di Windows

1. Install JDK 25 64-bit.
2. Buka PowerShell baru dan pastikan `java -version` menunjukkan versi 25.
3. Ekstrak proyek ke folder biasa, misalnya `C:\MinecraftMods\SkyLyrics-26.3`.
4. Jalankan `build.bat`.
5. Bila build dan semua tes berhasil, JAR akan berada di `build\libs\skylyrics-0.2.0.jar`.

Build pertama memerlukan koneksi internet untuk mengambil Gradle dan dependensi Fabric/Minecraft.

## Instalasi ekstensi Chrome / Edge

1. Pastikan Minecraft dengan SkyLyrics sudah berjalan. Mod membuka endpoint lokal di `127.0.0.1:38173`.
2. Buka `chrome://extensions` di Chrome atau `edge://extensions` di Edge.
3. Aktifkan **Developer mode**.
4. Klik **Load unpacked**.
5. Pilih folder `browser-extension` dari source project.
6. Buka `https://www.youtube.com` (video musik), `https://music.youtube.com`, atau `https://open.spotify.com` dan putar lagu.
7. Tekan `O` di Minecraft. Status sumber musik akan muncul pada tab Player.

Ekstensi mengirim metadata ke mod setiap detik. YouTube dan YouTube Music juga mengirim posisi pemutar untuk menyelaraskan lirik. Spotify Web Player hanya mengirim metadata dan status putar/jeda.

## Instalasi mod di TLauncher

1. Pasang Fabric Loader untuk Minecraft 26.3 dan jalankan profil tersebut sekali.
2. Tutup Minecraft.
3. Letakkan `skylyrics-0.2.0.jar` dan Fabric API yang sesuai di folder `mods` milik direktori game yang dipakai profil Fabric TLauncher.
4. Jalankan kembali profil Fabric 26.3.
5. Pasang ekstensi browser sesuai petunjuk di atas.
6. Buka YouTube, YouTube Music, atau Spotify Web Player dan mulai putar musik.
7. Tekan `O` untuk membuka panel SkyLyrics dan `H` untuk menampilkan/menyembunyikan HUD.

## Shortcut

| Tombol | Fungsi |
|---|---|
| `O` | Buka panel SkyLyrics |
| `H` | Tampilkan/sembunyikan HUD |
| `K` | Play/pause timer lokal; tidak mengendalikan pemutar eksternal |

Saat YouTube atau YouTube Music terdeteksi, tombol sinkronisasi di panel Player mengubah alignment lirik tanpa mengubah playback lagu.

## File LRC lokal

Folder library:

`config/skylyrics/lyrics/`

Contoh format:

```lrc
[ti:Song title]
[ar:Artist]
[offset:0]
[00:00.00]First line
[00:04.25]Second line
[00:08.60]Third line
```

Gunakan lirik yang kamu punya hak atau izin untuk gunakan. File LRC lokal tetap menjadi fallback saat pencarian online tidak menemukan lirik sinkron.

## Struktur proyek

```text
src/main/java/.../core/              Parser LRC, player clock, konfigurasi, library
src/main/java/.../integration/       Browser loopback server dan LRCLIB lookup
src/client/java/...                  Entrypoint, HUD, panel UI
src/test/java/...                    Tes parser dan external clock
browser-extension/                   Konektor Chrome / Edge
samples/                             LRC demo orisinal
```

## Batas verifikasi saat ini

- Core parser LRC dan player clock telah dikompilasi dengan Java lokal dan smoke test lulus.
- Source integration Java telah diperiksa menggunakan stub untuk dependensi Gson/SLF4J; bukan pengganti kompilasi terhadap classpath Minecraft sesungguhnya.
- Skrip JavaScript browser extension lulus `node --check`; manifest JSON valid.
- Build Fabric Loom final pada Minecraft 26.3 belum dijalankan karena hanya Java 21 tersedia dan dependensi Gradle tidak dapat diunduh.
- Belum dilakukan uji langsung pada TLauncher, YouTube, YouTube Music, atau Spotify Web Player di Windows.
- Kompatibilitas metadata bergantung pada browser yang menerbitkan Media Session dan DOM pemutar yang dapat dibaca ekstensi.

## Lisensi dan atribusi

Kode proyek dilisensikan MIT. SkyLyrics adalah proyek komunitas, bukan produk resmi Mojang/Microsoft dan tidak berafiliasi dengan TLauncher, Spotify, YouTube, atau LRCLIB.

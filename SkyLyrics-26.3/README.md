# SkyLyrics 0.4.0 — Minecraft Java 26.3

SkyLyrics adalah Fabric client mod untuk custom animated sky, lirik tersinkron, dan dashboard bergaya studio di Minecraft Java 26.3.

## Fitur 0.4.0

- Delapan keluarga font yang dapat dipilih dari menu **O → STYLES**: Poppins, Montserrat, Kanit, Fredoka, Unbounded, Bebas Neue, Pacifico, dan Caveat.
- Font pilihan diterapkan ke lirik world-space 3D dan lyrics HUD.
- Mini-player Now Playing permanen dengan artwork cover album, judul, artis, sumber musik, status playback, dan progress bar.
- Cover art diperoleh dari metadata Media Session browser; mod hanya mengunduh gambar HTTPS dari CDN YouTube/Spotify yang diizinkan, tanpa redirect, maksimal 1,5 MB dan 1024 × 1024.
- Animasi aksen ungu-pink yang halus pada dashboard dan indikator status play.
- Spotify Web Player: Now Playing + cover metadata saja; sinkronisasi lirik Spotify tetap dinonaktifkan.

## Sky dan lirik

- 20 preset sky: Aurora, Galaxy, Nebula Storm, Nebula, Sunset, Golden Sunset, Pink Sunset, Saturn, Meteors, Star Trails, Comet, Hyperspace, Blood Moon, Solar Flare, Dreamscape, Prism, Synthwave, Black Hole, Thunderstorm, dan Ocean.
- Efek GLSL prosedural: nebula bergerak, bintang berkedip, aurora, horizon glow, matahari, Saturn ring, comet, hyperspace streaks, black hole, dan lightning.
- Dashboard 8 tab: Overview, Skies, Lyrics, Styles, HUD, Performance, Settings, dan Information.
- Lirik dapat tampil sebagai floating text di dunia 3D atau HUD layar, dengan pilihan follow camera, fixed position, word reveal, glow, skala, dan sync offset.
- File LRC lokal dan sinkronisasi otomatis dari YouTube/YouTube Music.

## Font dan lisensi

Font diambil dari Google Fonts pada commit tetap `86aced91de05cd4f96a06838124cca45be337b2b`. Build Gradle mengunduh delapan file TTF dan delapan pemberitahuan SIL Open Font License (OFL). Lisensi disimpan di dalam JAR pada `META-INF/licenses/skylyrics-fonts/`.

Build pertama membutuhkan koneksi internet untuk mengunduh berkas font. Font tidak diunduh saat game berjalan.

## Persyaratan

- Minecraft Java 26.3.
- Fabric Loader 0.20.1 atau lebih baru.
- Fabric API 0.165.2+26.3.
- Java Runtime 25 atau lebih baru.
- Chrome atau Edge untuk integrasi media browser.

## Instalasi

1. Pasang Fabric Loader untuk Minecraft 26.3 dan jalankan profil Fabric sekali.
2. Tutup Minecraft.
3. Letakkan `skylyrics-0.4.0.jar` dan Fabric API yang sesuai pada folder `mods` di direktori game TLauncher.
4. Jalankan kembali profil Fabric 26.3.
5. Untuk koneksi browser, buka `chrome://extensions` atau `edge://extensions`, aktifkan Developer mode, lalu pilih **Load unpacked** dan arahkan ke folder `browser-extension`.
6. Putar musik di YouTube, YouTube Music, atau Spotify Web Player.

## Shortcut

| Tombol | Fungsi |
|---|---|
| `O` | Buka dashboard SkyLyrics |
| `H` | Tampilkan/sembunyikan lyrics HUD |
| `K` | Play/pause timer lirik lokal |

## Library LRC

Simpan file `.lrc` di folder `config/skylyrics/lyrics/`. Gunakan file lirik yang memang kamu punya hak atau izin untuk digunakan.

## Privasi dan artwork

Ekstensi mengirim metadata musik ke server lokal `127.0.0.1:38173`. Tidak ada audio yang direkam, diunduh, atau dikirim. Artwork hanya diterima dari host CDN HTTPS yang diizinkan; download gambar dibatasi ukuran, tipe konten, dan waktu respons. Server lokal tidak menerima perintah kontrol pemutar.

- YouTube dan YouTube Music: metadata, posisi pemutaran, dan pencarian lirik tersinkron.
- Spotify Web Player: metadata Now Playing serta cover art; sinkronisasi lirik Spotify dinonaktifkan.
- SkyLyrics adalah client-side mod. Server multiplayer tidak perlu memasangnya.

## Build dan pemeriksaan

CI menggunakan JDK 25 dan Gradle 9.8.2. CI memeriksa kompilasi, unit tests, 20 file sky post-effect, empat shader stages, delapan definisi font, delapan file TTF hasil unduhan, delapan lisensi OFL, serta sintaks ekstensi browser.

Build CI yang sukses belum menggantikan uji visual langsung dalam TLauncher/Minecraft 26.3. Tampilan dapat berbeda tergantung GPU, resolusi, dan kombinasi resource pack.

## Lisensi

Kode SkyLyrics: MIT. Delapan keluarga font memiliki lisensi SIL Open Font License masing-masing dan pemberitahuan lisensinya disertakan di JAR. SkyLyrics tidak berafiliasi dengan Mojang/Microsoft, TLauncher, YouTube, Spotify, atau LRCLIB.

# SkyLyrics 0.3.0 — Minecraft Java 26.3

SkyLyrics adalah Fabric client mod untuk custom animated sky, lirik tersinkron, dan dashboard bergaya studio di Minecraft Java 26.3.

## Fitur 0.3.0

- 20 preset visual sky: Aurora, Galaxy, Nebula Storm, Nebula, Sunset, Golden Sunset, Pink Sunset, Saturn, Meteors, Star Trails, Comet, Hyperspace, Blood Moon, Solar Flare, Dreamscape, Prism, Synthwave, Black Hole, Thunderstorm, dan Ocean.
- Efek procedural GLSL: nebula bergerak, bintang berkedip, aurora, horizon glow, matahari, Saturn ring, comet, hyperspace streaks, black hole, dan lightning.
- Dashboard 8 tab: Overview, Skies, Lyrics, Styles, HUD, Performance, Settings, dan Information.
- Lirik dapat tampil sebagai world-space floating text 3D atau HUD layar.
- Pengaturan follow-camera atau fixed position, jarak, skala, word reveal, dan glow.
- Enam style warna: Lavender, Dreamy, Comic, Script, Ice, dan Royal.
- LRC lokal dan koneksi metadata YouTube/YouTube Music melalui ekstensi Chrome/Edge.
- Spotify Web Player hanya untuk Now Playing metadata; sinkronisasi lirik Spotify tidak diaktifkan.

## Status kesesuaian dengan video referensi

Versi 0.3.0 sudah membangun fondasi utama yang terlihat di video: preset sky, efek bergerak, dashboard delapan tab, dan lirik floating 3D. Versi ini belum identik 100%. Font eksternal seperti Poppins/Montserrat, cover album pada mini-player, dan sebagian detail UI/animasi lanjutan belum tersedia. Efek langit dihitung secara procedural oleh shader, bukan melalui sekumpulan gambar statis.

## Persyaratan

- Minecraft Java 26.3.
- Fabric Loader 0.20.1 atau lebih baru.
- Fabric API 0.165.2+26.3.
- Java Runtime 25 atau lebih baru.
- Chrome atau Edge untuk integrasi media browser.

## Instalasi

1. Pasang Fabric Loader untuk Minecraft 26.3.
2. Jalankan profil Fabric satu kali, lalu tutup Minecraft.
3. Letakkan file skylyrics-0.3.0.jar dan Fabric API yang sesuai ke folder mods pada direktori game TLauncher.
4. Jalankan kembali profil Fabric 26.3.
5. Untuk integrasi browser, buka halaman ekstensi Chrome/Edge, aktifkan Developer mode, pilih Load unpacked, lalu arahkan ke folder browser-extension dari source repository.
6. Putar musik di YouTube atau YouTube Music, lalu buka menu SkyLyrics di Minecraft.

## Shortcut

| Tombol | Fungsi |
|---|---|
| O | Buka dashboard SkyLyrics |
| H | Tampilkan/sembunyikan lyrics HUD |
| K | Play/pause timer lirik lokal |

## File LRC lokal

Simpan file LRC di folder config/skylyrics/lyrics/.

Contoh isi file:

    [ti:Song title]
    [ar:Artist]
    [offset:0]
    [00:00.00]First line
    [00:04.25]Second line
    [00:08.60]Third line

## Integrasi browser dan privasi

Ekstensi mengirim metadata sumber, judul, artis, album, status playback, dan waktu pemutar ke endpoint lokal 127.0.0.1:38173. Audio tidak direkam atau dikirim.

- YouTube dan YouTube Music: metadata, posisi playback, dan pencarian lirik tersinkron.
- Spotify Web Player: metadata Now Playing saja. Sinkronisasi lirik Spotify dinonaktifkan.
- SkyLyrics adalah client-side mod; server multiplayer tidak perlu memasangnya.

## Build dan pengujian

Build CI menggunakan JDK 25 dan Gradle 9.8.2. Pipeline menjalankan unit tests, memeriksa 20 file post-effect JSON dan empat shader stages, membangun JAR, serta mengunggah artifact GitHub Actions.

Build CI berhasil tidak sama dengan uji visual langsung. Uji masuk ke dunia TLauncher/Minecraft, kecocokan shader pada GPU pengguna, dan perilaku runtime tetap perlu smoke test di Minecraft 26.3.

## Lisensi

Kode proyek dilisensikan MIT. Font pihak ketiga belum dibundel pada versi ini. SkyLyrics tidak berafiliasi dengan Mojang/Microsoft, TLauncher, Spotify, YouTube, atau LRCLIB.

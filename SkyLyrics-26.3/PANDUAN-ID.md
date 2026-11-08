# Panduan Instalasi dan Penggunaan SkyLyrics 0.11.0

SkyLyrics 0.11.0 adalah pemutar musik **langsung di dalam Minecraft**. Tidak perlu membuka Spotify, YouTube, Chrome, browser lain, atau pemutar musik eksternal.

## 1. Persyaratan

- Minecraft Java Edition **26.3**.
- Fabric Loader **0.20.1 atau lebih baru**.
- Fabric API **0.165.2+26.3** atau versi kompatibel untuk Minecraft 26.3.
- Java **25 atau lebih baru**.
- Windows atau laptop/PC dengan perangkat keluaran audio.

## 2. Pasang di laptop

1. Jalankan profil Minecraft 26.3 Fabric sekali, lalu tutup game.
2. Buka folder instalasi Minecraft yang dipakai oleh launcher.
3. Masukkan `skylyrics-0.11.0.jar` ke folder `mods`.
4. Pastikan Fabric API untuk Minecraft 26.3 juga ada di folder `mods`.
5. Jalankan profil Fabric 26.3.

SkyLyrics adalah mod **client-side**. Server multiplayer tidak perlu memasang mod ini. Pemain yang memasangnya sendiri dapat membuka Music Center di client mereka.

## 3. Buka Music Center saat bermain

1. Masuk ke dunia single-player atau server multiplayer.
2. Tekan tombol **O** pada keyboard.
3. Music Center muncul sebagai layar di dalam Minecraft. Dunia tetap berjalan; layar SkyLyrics tidak menghentikan permainan.
4. Tab yang terbuka pertama kali adalah **Music Center**.

Tidak perlu mengetik command di chat.

## 4. Putar lagu dari laptop — sepenuhnya di dalam game

1. Di Music Center tekan **IMPORT SONG**.
2. Penjelajah file SkyLyrics akan muncul di dalam Minecraft. Ini bukan jendela Windows dan bukan pemutar musik eksternal.
3. Gunakan shortcut **HOME**, **MUSIC**, **DOWNLOADS**, **DESKTOP**, atau **UP** untuk mencari folder lagu.
4. Klik file audio yang ingin diputar.
5. SkyLyrics menyalin file ke `config/skylyrics/music/` dan mulai memutarnya melalui audio engine di dalam Minecraft.

Format yang didukung: **MP3, OGG, WAV, AIFF, dan AU**.

Kontrol yang tersedia di Music Center: Previous, Play/Pause, Stop, Next, Seek mundur/maju 10 detik, Volume turun/naik, serta impor file LRC.

### Kelola library, favorit, dan playlist

1. Buka tab **OFFLINE LIBRARY**. Gunakan kolom **SEARCH** untuk mencari nama file lagu.
2. Tombol **FILTER** berganti antara semua lagu, favorit, dan playlist aktif. Tombol **PLAYLIST** berganti ke playlist yang tersedia.
3. Tekan **☆ / ★** di samping lagu untuk menambah atau menghapus favorit. Tekan **+ / −** untuk menambah atau menghapus lagu dari playlist aktif.
4. Ketik nama playlist di kolom bawah lalu tekan **CREATE**. Playlist aktif dapat dihapus melalui **DELETE ACTIVE PLAYLIST**.
5. **SHUFFLE** mengacak antrean. **REPEAT** berganti Off → All → One. Pengaturan ini tersimpan dan digunakan saat lagu selesai diputar.

## 5. Tampilkan lirik tersinkron

1. Siapkan file `.lrc` yang cocok dengan lagu.
2. Pilih **IMPORT / MATCH LRC** di Music Center atau **IMPORT .LRC LYRICS** di Offline Library.
3. Pilih file melalui penjelajah file SkyLyrics di dalam game.
4. Untuk pencocokan otomatis, nama dasar file audio dan LRC sebaiknya sama. Contoh: `Song.mp3` dan `Song.lrc`.
5. Pilih tampilan **Floating World Text (3D)** atau **Screen HUD (2D)** pada halaman Lyrics.

LRC bertimestamp baris didukung. Timestamp per kata digunakan saat datanya tersedia; jika tidak, lirik tetap memakai sinkronisasi per baris.

## 6. Tombol bawaan

| Tombol | Fungsi |
|---|---|
| O | Buka Music Center |
| H | Tampilkan/sembunyikan HUD lirik |
| K | Play/Pause |
| N | Lagu berikutnya |
| B | Lagu sebelumnya |

Tombol dapat diubah dari **O → KEYBINDS** atau Minecraft **Options → Controls → Key Binds → SkyLyrics**.

## 7. Lokasi file

- Musik: `config/skylyrics/music/`
- Lirik: `config/skylyrics/lyrics/`
- Riwayat pemutaran: `config/skylyrics/history.json`
- Playlist, favorit, dan preferensi antrean: `config/skylyrics/library-state.json`

File audio disalin ke folder library SkyLyrics; file asli di folder laptop tidak dihapus.

## 8. Batas pengujian

Build dan tes otomatis dicatat pada `QA_STATUS.md`. Suara dari perangkat laptop, navigasi file in-game, dan interaksi layar tetap perlu diuji langsung di Minecraft 26.3 pada laptop yang digunakan.

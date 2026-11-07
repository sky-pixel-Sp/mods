# Panduan Instalasi dan Penggunaan SkyLyrics 0.9.1

**Pembuat:** Sky  
**Target:** Minecraft Java 26.3 · Fabric Loader 0.20.1+ · Java 25+

## 1. Pasang mod di TLauncher

1. Ekstrak paket ZIP ini.
2. Buka folder `SkyLyrics-0.9.1-Minecraft-26.3`.
3. Salin file `skylyrics-0.9.1.jar` ke folder `mods` milik profil Minecraft 26.3 Fabric di TLauncher.
4. Pastikan Fabric API yang cocok untuk Minecraft 26.3 juga ada di folder `mods`.
5. Jalankan profil Fabric 26.3, masuk ke dunia, lalu tekan **O** untuk membuka SkyLyrics.

Mod ini bersifat client-side. Server multiplayer tidak perlu memasangnya.

## 2. Putar musik lokal

1. Tekan **O** → **Music Center** → **Import Song**.
2. Pilih file MP3, OGG, atau WAV dari komputer.
3. SkyLyrics menyalin file ke `config/skylyrics/music/` agar muncul di **Offline Library**.
4. Gunakan tombol Play/Pause, Previous, Next, Seek, dan Volume di menu.

Audio lokal diputar melalui player SkyLyrics pada komputer yang menjalankan Minecraft. Musik tetap dapat diputar tanpa internet setelah file diimpor.

## 3. Tampilkan lirik

1. Siapkan file lirik format **.LRC** yang cocok dengan lagu.
2. Buka **O** → **Lyrics** → **Import / Match LRC**.
3. Pilih file LRC. Untuk pencocokan otomatis, nama dasar file lirik harus sama dengan nama file audio.
4. Pilih **Floating World Text** untuk lirik 3D di dunia Minecraft atau **Screen HUD** untuk lirik di layar.
5. Atur gaya teks, font, ukuran, dan sinkronisasi di menu **Lyrics** dan **Styles**.

## 4. Kontrol Spotify Web / YouTube Music

1. Buka **O** → **Online Music**.
2. Klik **OPEN SPOTIFY WEB** atau **OPEN YOUTUBE MUSIC**.
3. Login langsung di halaman resmi penyedia musik pada Chrome atau Edge, lalu mulai putar lagu.
4. Buka `chrome://extensions` (Chrome) atau `edge://extensions` (Edge).
5. Aktifkan **Developer mode**.
6. Klik **Load unpacked**, lalu pilih folder `browser-extension` dari paket ZIP ini.
7. Kembali ke tab Spotify / YouTube Music dan refresh halaman. Biarkan tab tersebut terbuka saat bermain.
8. SkyLyrics akan menerima metadata lagu dan mengirim kontrol pemutaran ke tab browser.

**Penting:** audio dari Spotify / YouTube Music tetap diputar oleh browser, bukan dialirkan ulang melalui audio engine Minecraft. Spotify Web saat ini menyediakan metadata dan kontrol saja; sinkronisasi lirik Spotify tidak diaktifkan. Jangan pernah memasukkan password akun Spotify ke dalam mod atau ekstensi.

## 5. Tombol bawaan

| Tombol | Fungsi |
|---|---|
| **O** | Buka / tutup Music Center |
| **H** | Tampilkan / sembunyikan lyric HUD |
| **K** | Play / Pause |
| **N** | Lagu berikutnya |
| **B** | Lagu sebelumnya |

Tombol bisa diubah dari **O** → **Keybinds** atau melalui Options → Controls di Minecraft.

## 6. Jika ada masalah

- **Menu tidak terbuka:** buka Options → Controls dan pastikan tombol SkyLyrics tidak bentrok dengan mod lain.
- **Offline Library kosong:** pastikan impor selesai dan file berada di `config/skylyrics/music/`.
- **Audio tidak terdengar:** periksa output audio Windows, volume SkyLyrics, dan apakah file audio dapat diputar normal di pemutar lain.
- **Lirik tidak muncul:** pastikan file berekstensi .LRC dan namanya cocok dengan file audio.
- **Kontrol browser tidak tersambung:** pastikan ekstensi sudah aktif, tab musik terbuka, lalu refresh halaman penyedia.

Kalau masalah tetap terjadi, simpan log Minecraft dari folder `logs/latest.log` dan sertakan pesan error yang muncul.

## 7. Folder di dalam paket

- `skylyrics-0.9.1.jar` — mod Fabric yang dipasang ke Minecraft.
- `browser-extension/` — ekstensi Chrome / Edge untuk menghubungkan pemutar musik browser.
- `README.md` — dokumentasi lengkap proyek.
- `LICENSE` — lisensi MIT untuk kode SkyLyrics.

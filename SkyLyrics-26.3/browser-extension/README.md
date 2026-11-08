# SkyLyrics Media Connector — Chrome / Edge

## Install

1. Jalankan Minecraft 26.3 dengan SkyLyrics 0.9.2. Mod harus aktif agar endpoint lokal tersedia pada `127.0.0.1:38173`.
2. Buka `chrome://extensions` di Chrome atau `edge://extensions` di Edge.
3. Aktifkan **Developer mode**.
4. Pilih **Load unpacked** dan pilih folder `browser-extension` ini.
5. Buka salah satu: video musik di `https://www.youtube.com/watch…`, YouTube Music (`https://music.youtube.com`), atau Spotify Web Player (`https://open.spotify.com`).
6. Putar musik, lalu cek tab Player di menu SkyLyrics (tombol `O`).

YouTube dan YouTube Music mengirim metadata serta posisi pemutar untuk pencarian dan sinkronisasi LRC otomatis saat LRCLIB memiliki kecocokan yang tepat. Spotify Web Player hanya mengirim metadata dan status playback; sinkronisasi lirik dinonaktifkan. Konektor tidak membaca atau mengirim audio.

Konektor ini untuk pemutar web pada Chrome/Edge. Aplikasi Spotify desktop tidak dijamin terdeteksi. Jika endpoint lokal belum aktif, pastikan Minecraft sudah berjalan dan SkyLyrics berhasil dimuat. Ekstensi mengirim ulang metadata setiap detik.

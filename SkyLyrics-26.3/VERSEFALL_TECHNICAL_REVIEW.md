# Pemeriksaan teknis VerseFall 1.1.0 dan keputusan implementasi SkyLyrics

Dokumen ini mencatat hasil pemeriksaan arsip JAR VerseFall yang diberikan pengguna. Tujuannya memahami subsistem dan perilaku mod, bukan menyalin implementasi atau asetnya.

## 1. Identitas dan kompatibilitas arsip

Metadata internal JAR:
- Mod ID: `spotify_mod`
- Nama: Versefall
- Versi: `1.0.0+mc1.19.4`
- Entrypoint: `com.spotifyclient.SpotifyModClient`
- Minecraft target: Java 1.19.4
- Fabric Loader minimum: 0.14.19
- Java minimum: 17

Inventaris arsip yang diperiksa berisi sekitar 216 class Java, 73 file JSON, 15 suara OGG, 10 font TTF, dan 10 tekstur PNG. Shader tersebar dalam berkas fragment dan vertex, termasuk renderer glass dan post-effect. Arsip ini **tidak kompatibel langsung** dengan Minecraft Java 26.3 yang digunakan SkyLyrics. API render, UI dan input sudah berubah.

## 2. Peta paket dan tanggung jawab

Paket-paket utama yang terdeteksi:
- `com.spotifyclient`: entrypoint, pengelola state dan konfigurasi.
- `music`: sesi musik, visualizer, efek pulau/Now Playing, album glow dan music notes.
- `lyrics`: pencarian, parsing, cache dan pemilihan baris lirik.
- `media`: sumber media lokal dan pembaca Windows media session.
- `hud`: abstraksi komponen HUD yang dapat digeser, posisi dan animasi drag.
- `gui` / `ui`: layar pengaturan, HUD editor, Click GUI dan bind editor.
- `render`: tipografi, lapisan lirik, shader glass dan renderer 2D/3D.
- `theme`: warna, font dan gaya antarmuka.
- `mixin`, `compat`: integrasi ke lifecycle dan abstraksi versi Minecraft.
- `combat`, `fun`, `viewmodel`, `cape`: modul lain di luar ruang lingkup musik/lirik.

Entry point client menginisialisasi service media, lyrics, HUD, modul, keybind dan pengaturan; subsistem kemudian memperbarui state melalui polling/event client dan merender komponen pada tahap HUD/world rendering.

## 3. Cara kerja integrasi Spotify dan media Windows

### Spotify Web API

Class yang diperiksa menunjukkan flow OAuth Authorization Code dengan PKCE:
1. Mod membuat verifier dan challenge PKCE, serta state acak.
2. Pengguna diarahkan ke halaman otorisasi Spotify.
3. Server callback lokal menerima redirect pada loopback `127.0.0.1:43821/callback`.
4. State diverifikasi dan code ditukar ke access token/refresh token.
5. Access token dipakai membaca currently-playing endpoint Spotify.
6. Respons metadata memberi judul, artis, album, artwork, status playback dan posisi lagu.
7. Token diperbarui saat diperlukan; respons 401/403/429 ditangani dengan refresh atau jeda.

Implementasi arsip menyimpan token di `config/spotify_token.txt` sebagai data teks/JSON. Itu berarti token sensitif berada di disk tanpa perlindungan enkripsi aplikasi yang terlihat dari arsip. SkyLyrics tidak menyalin pola penyimpanan token tersebut.

### Windows Global System Media Transport Controls

`WindowsMediaReader` menggunakan JNA/COM untuk mengakses Windows Global System Media Transport Controls Session Manager. Ia mencari sesi Spotify, membaca media properties (title, artist, album), timeline, posisi dan playback state. `LocalMediaSource` menjalankan polling berkala sekitar 500 ms dan melakukan jeda/retry beberapa detik bila sesi tidak tersedia.

Penting: alur ini membaca metadata serta status transport Windows; bukan menangkap audio PCM dari Spotify. Audio tetap dikeluarkan oleh aplikasi pemutar.

## 4. Cara kerja pencarian dan parser lirik

Lyrics manager menggunakan LRCLIB REST API:
- mencari dengan judul, artis, variasi judul yang dibersihkan dan durasi;
- memeriksa kandidat, termasuk kecocokan durasi;
- memilih `syncedLyrics` bila tersedia;
- memakai cache hasil yang diketahui;
- menjalankan permintaan di thread daemon dan generation counter untuk menghindari hasil lama menimpa lagu baru;
- membatasi retry saat provider membalas 429.

Dari bytecode terlihat dua pola penting:
- `[mm:ss.xx]` untuk timestamp baris LRC;
- `<mm:ss.xxx>` untuk timestamp kata di dalam baris.

Parser memetakan waktu kata ke indeks kata, menghapus tag timestamp dari teks yang ditampilkan, lalu menentukan kata aktif berdasarkan posisi lagu. Komponen Lyrics Bar menggunakan peta itu untuk mengisi warna karaoke secara progresif. Lirik biasa tanpa tag kata tetap berjalan sebagai lirik per baris.

**Penerapan di SkyLyrics 0.7.0:** `LyricsTrack` kini membaca tag kata inline dan menyimpan `WordTiming(timeMs, wordIndex)` pada `LyricsCue`. HUD layar memberi warna berbeda pada kata yang telah/ sedang diucapkan, sedangkan 3D lyrics mengungkapkan kata berdasarkan timestamp asli. LRC standar tetap memakai fallback line-progress yang lama. Pengujian unit mencakup indeks kata aktif, multi-timestamp baris, dan lirik standar.

## 5. Arsitektur HUD drag-and-drop

Bagian paling khas VerseFall adalah abstraksi HUD yang terpisah dari komponen rendering:
- `Draggable` memberi tiap widget ID stabil, posisi default, ukuran, hitbox dan perilaku interaktif.
- `DragController` memegang posisi target, offset grab, state dragging/cancel, interpolation/follow animation, tilt dan ghost preview.
- `DragSystem` mengelola daftar widget, hit testing, drag mode, reset, posisi asal dan penyimpanan.
- `HudEditorScreen` memberi kanvas, petunjuk drag, reset dengan R, simpan saat keluar dan interaksi mouse.
- `NowPlayingComp` dan `LyricsBarComp` adalah komponen terpisah; masing-masing dapat punya animasi, artwork, visualizer, kontrol transport dan layout sendiri.

Posisi disimpan berdasarkan ID komponen sehingga layout dapat dipulihkan. Drag system menguji elemen dari atas ke bawah, membatasi widget agar tidak hilang dari layar, mengenali double-click untuk reset ke posisi default, dan menyediakan right-click untuk membatalkan drag.

**Penerapan di SkyLyrics 0.7.0:** SkyLyrics mendapatkan HUD Layout Studio dengan dua komponen independen (Now Playing dan Lyrics HUD). Keduanya memakai koordinat layar ternormalisasi sehingga layout bertahan saat resolusi berubah. Drag kiri memindahkan panel, double-click pada panel mengembalikan panel itu saja, right-click membatalkan perubahan drag yang sedang berlangsung, dan Reset All mengembalikan dua posisi default. Pengaturan disimpan di `config/skylyrics.properties` dengan migrasi konfigurasi versi 3.

Ruang lingkupnya sengaja dimulai dengan dua panel utama; bukan klaim bahwa semua widget kecil VerseFall sudah direplikasi.

## 6. Rendering dan efek visual

Arsip VerseFall mencakup renderer 2D berbasis MSDF, layer teks 3D, shader glass/blur, komponen artwork, pulse/tilt/wave, island-style Now Playing, audio visualizer dan beberapa efek musik. Sebagian efek bergantung pada pipeline serta resource format Minecraft 1.19.4.

SkyLyrics punya renderer dan pipeline sendiri untuk Minecraft 26.3:
- Now Playing card dengan artwork/warna aksen;
- waveform offline berdasarkan RMS PCM dan animasi transport online (audio browser tidak ditangkap);
- lirik HUD maupun tipografi 3D dengan glow/outline;
- post-effect sky yang independen dari audio player.

Tidak ada shader, font, gambar, suara, atau class VerseFall yang dibundel atau dipindahkan ke SkyLyrics. Aset visual SkyLyrics tetap milik/berlisensi sumber masing-masing.

## 7. Perbandingan perilaku

| Area | VerseFall 1.1.0 | SkyLyrics 0.7.0 |
|---|---|---|
| Target game | Minecraft 1.19.4 | Minecraft 26.3 |
| Online Spotify | OAuth PKCE + Web API | Browser player + ekstensi loopback, tanpa token Spotify yang disimpan SkyLyrics |
| Media Windows | Windows SMTC/JNA reader | Belum memakai Windows SMTC; menerima metadata dari ekstensi browser |
| Lirik | LRCLIB, line & word timestamp | LRCLIB untuk YouTube, line & word timestamp untuk file/provider LRC |
| Karaoke per kata | Timestamp kata | Timestamp kata + highlight HUD + reveal 3D |
| HUD layout | Banyak widget draggable | Now Playing + Lyrics HUD draggable melalui layar editor |
| Penyimpanan posisi | ID widget dalam config | Koordinat ternormalisasi di config SkyLyrics |
| Shader/visual | MSDF, glass blur, music island, visualizer | Renderer independen 26.3, font sendiri, artwork glow, waveform, sky effects |

Kesimpulan: **SkyLyrics bukan salinan VerseFall dan belum identik dalam semua subsystem.** Persamaan yang sekarang dikejar adalah perilaku inti musik/lirik, pengaturan visual, per-kata karaoke dan HUD layout. Integrasi Spotify OAuth native dan Windows SMTC masih merupakan perbedaan nyata.

## 8. Batas lisensi dan keamanan

Metadata mod di JAR memuat nilai MIT, tetapi arsip yang diberikan tidak menyertakan file LICENSE proyek di root; font Aquire yang dibundel mempunyai pemberitahuan lisensi non-komersial, dan halaman distribusi proyek yang dirujuk pengguna menampilkan pernyataan hak yang berbeda. Karena ada ketidakkonsistenan tersebut, arsip ini diperlakukan sebagai referensi analisis saja.

SkyLyrics tidak mendistribusikan ulang kode hasil dekompilasi, font, shader, texture, ikon atau audio dari VerseFall. Implementasi 0.7.0 dibuat independen dari API Minecraft 26.3 dan hanya memakai konsep fitur umum: pengelolaan media, timestamp kata, layout HUD yang dapat dipindah, dan visual feedback.

Selain itu, menyimpan OAuth refresh token sebagai plaintext bukan pola yang diadopsi oleh SkyLyrics. SkyLyrics memakai ekstensi browser lokal untuk perintah dan metadata media; audio provider tetap berada di browser dan tidak ditangkap mod.

## 9. File SkyLyrics yang berubah di 0.7.0

- `core/LyricsCue.java`: struktur timing kata dan pencarian kata aktif.
- `core/LyricsTrack.java`: parser LRC/LRCLIB line + word timestamp.
- `core/SkyLyricsConfig.java`: posisi HUD independen serta migrasi config versi 3.
- `render/SkyLyricsHud.java`: posisi Now Playing/Lyrics dan karaoke highlighting.
- `render/SkyLyricsWorldText.java`: reveal 3D berbasis word timestamp.
- `ui/HudEditorScreen.java`: layar drag-and-drop.
- `ui/SkyLyricsScreen.java`: tombol pembuka HUD Layout Studio.
- `src/test`: uji parser karaoke dan persistensi/ migrasi posisi HUD.

CI hijau membuktikan compile, test dan packaging. Pengujian manual audio, interaksi mouse, layout di resolusi berbeda, dan pembukaan mod di TLauncher tetap harus dilakukan di komputer pengguna sebelum dinyatakan terverifikasi penuh.

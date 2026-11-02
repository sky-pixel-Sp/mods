# SkyDesk for Windows

Aplikasi desktop produktivitas ringan dengan UI berbahasa Indonesia.

## Fitur
- Dashboard ringkas untuk tugas, catatan, dan sesi fokus.
- Task manager dengan prioritas dan penanda selesai.
- Catatan multi-dokumen dengan penyimpanan otomatis.
- Focus timer 25/50 menit dan istirahat 5/15 menit.
- Backup JSON ekspor/impor.
- Data disimpan lokal di `%APPDATA%\\SkyDesk\\data.json`.

## Build otomatis Windows
Setiap push ke branch `skydesk-windows-build` menjalankan GitHub Actions:
1. Tes unit penyimpanan.
2. Pemeriksaan sintaks Python.
3. Build `SkyDesk.exe` dengan PyInstaller di runner Windows.
4. Unggah `SkyDesk.exe` sebagai artifact bernama `SkyDesk-Windows-x64`.

Buka tab **Actions**, pilih workflow **Build SkyDesk for Windows**, lalu unduh artifact pada run yang selesai dengan status hijau.

## Jalankan dari source di Windows
Install Python 3.12 atau lebih baru, buka terminal di folder ini, lalu:
```powershell
python -m unittest discover -s tests -v
python app.py
```

## Catatan
- Build dijalankan oleh GitHub Actions; file EXE hanya dinyatakan berhasil setelah workflow selesai.
- Timer hanya berjalan saat aplikasi sedang terbuka.
- Data tidak disinkronkan ke cloud.

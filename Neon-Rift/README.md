# NEON RIFT — Survival Protocol

A neon sci-fi survival arcade game for Windows. Built with Python's standard Tkinter library; no account, network, or extra game assets required.

## Play
- **WASD / Arrow keys:** move.
- **Hold left mouse button:** fire toward the mouse cursor.
- **Hold Space:** fire with automatic nearest-enemy targeting.
- **Esc:** pause/resume.
- **R / Enter:** retry after a run.
- Collect green repair, gold overdrive, and cyan shield drops.
- Waves get denser over time; boss enemies appear every fifth wave.
- Your personal-best score is saved locally in `%APPDATA%\NeonRift\profile.json`.

## Windows executable
The GitHub Actions workflow tests game helpers and builds a standalone Windows x64 executable named `NeonRift.exe`. Download artifact **Neon-Rift-Windows-x64** from the successful workflow run.

## Run from source
Install Python 3.12 or newer:
```powershell
python -m unittest discover -s tests -v
python app.py
```

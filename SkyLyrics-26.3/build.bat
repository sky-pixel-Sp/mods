@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if errorlevel 1 (
  echo.
  echo Build failed. Read the error above, fix the requirement, then run build.bat again.
  pause
  exit /b 1
)
echo.
echo Build complete. Press any key to close.
pause >nul

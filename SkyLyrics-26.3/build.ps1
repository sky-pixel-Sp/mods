$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$GradleVersion = '9.6.0'
$ToolsDir = Join-Path $ProjectRoot '.tools'
$GradleHome = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleHome 'bin\gradle.bat'

Write-Host '=== SkyLyrics build check ===' -ForegroundColor Cyan
$javaVersionText = (& java -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersionText -notmatch 'version "(?:25|2[6-9]|[3-9]\d)') {
    Write-Host 'ERROR: Java 25 or newer is required for Minecraft 26.3.' -ForegroundColor Red
    Write-Host 'Install a 64-bit JDK 25, reopen PowerShell, then run build.bat again.'
    exit 2
}
Write-Host (($javaVersionText -split "`n")[0]) -ForegroundColor Green

if (-not (Test-Path $GradleExe)) {
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    $Zip = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
    $Url = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
    Write-Host "Downloading official Gradle $GradleVersion ..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $Url -OutFile $Zip
    Expand-Archive -Path $Zip -DestinationPath $ToolsDir -Force
}

Push-Location $ProjectRoot
try {
    & $GradleExe --no-daemon clean test build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host ''
    Write-Host 'BUILD SUCCESS' -ForegroundColor Green
    Write-Host 'Mod JAR: build\libs\skylyrics-0.2.0.jar' -ForegroundColor Green
} finally {
    Pop-Location
}

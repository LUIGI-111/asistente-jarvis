# Compila la app con el JDK portable de .tools (incluye JavaFX).
$ErrorActionPreference = 'Stop'
$jdk = Get-ChildItem "$PSScriptRoot\.tools" -Directory -Filter 'jdk-*' | Select-Object -First 1
if (-not $jdk) { throw 'No se encontró el JDK en app\.tools' }
$out = Join-Path $PSScriptRoot 'out'
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory $out | Out-Null
$fuentes = Get-ChildItem "$PSScriptRoot\src" -Recurse -Filter *.java | ForEach-Object FullName
& "$($jdk.FullName)\bin\javac.exe" -encoding UTF-8 --add-modules javafx.controls -d $out $fuentes
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación' }
Write-Host "Compilado en $out"

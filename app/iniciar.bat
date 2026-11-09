@echo off
rem Lanza la pestaña del asistente (sin consola). Compila antes con compilar.ps1 si cambias el código.
cd /d "%~dp0"
for /d %%J in ("%~dp0.tools\jdk-*") do set "JDK=%%J"
start "" "%JDK%\bin\javaw.exe" -Dfile.encoding=UTF-8 --add-modules javafx.controls -cp out jarvis.Main

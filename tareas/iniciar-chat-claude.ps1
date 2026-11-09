# Abre un chat de Claude Code en ASISTENTE al iniciar sesion en Windows (+30 s).
# Para desactivarlo: Disable-ScheduledTask -TaskName "Chat Claude ASISTENTE al iniciar sesion"
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre  = "Chat Claude ASISTENTE al iniciar sesion"
$script  = (Resolve-Path (Join-Path $PSScriptRoot "..\automatizacion\abrir-chat-claude.ps1")).Path
$usuario = "$env:USERDOMAIN\$env:USERNAME"

$action   = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoExit -NoProfile -ExecutionPolicy Bypass -File `"$script`""
$trigger  = New-ScheduledTaskTrigger -AtLogOn -User $usuario
$trigger.Delay = "PT30S"
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Hours 0)

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger $trigger -Settings $settings `
    -Description "Abre un chat de Claude Code en la carpeta ASISTENTE al iniciar sesion" -Force | Out-Null

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State

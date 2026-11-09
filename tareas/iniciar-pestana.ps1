# Abre la pestaña Jarvis automaticamente al iniciar sesion en Windows (con 15 s de espera).
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre = "Pestana Jarvis al iniciar sesion"
$bat    = (Resolve-Path (Join-Path $PSScriptRoot "..\app\iniciar.bat")).Path
$usuario = "$env:USERDOMAIN\$env:USERNAME"

$action   = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$bat`""
$trigger  = New-ScheduledTaskTrigger -AtLogOn -User $usuario
$trigger.Delay = "PT15S"
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Minutes 5)

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger $trigger -Settings $settings `
    -Description "Abre la pestaña flotante del asistente al iniciar sesion" -Force | Out-Null

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State

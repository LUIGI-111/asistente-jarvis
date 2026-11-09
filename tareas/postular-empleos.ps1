# Registra la tarea "Postulaciones de empleo": lunes a viernes 9:00 y al iniciar sesión (+5 min).
# automatizacion\postular-empleos.ps1 lo hace una sola vez al día: gana el primer disparador.
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre  = "Postulaciones de empleo"
$dias    = "Monday","Tuesday","Wednesday","Thursday","Friday"
$script  = (Resolve-Path (Join-Path $PSScriptRoot "..\automatizacion\postular-empleos.ps1")).Path
$usuario = "$env:USERDOMAIN\$env:USERNAME"

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$script`""
$t1 = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $dias -At "9:00AM"
$t2 = New-ScheduledTaskTrigger -AtLogOn -User $usuario
$t2.Delay = "PT5M"
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 60)

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger @($t1, $t2) -Settings $settings `
    -Description "Claude busca vacantes nuevas en LinkedIn, filtra con CONTEXTO-BUSQUEDA-EMPLEO.md y envia las postulaciones que calzan: L-V 9:00 y al iniciar sesion (una vez al dia)" -Force | Out-Null

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State

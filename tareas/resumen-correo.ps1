# Resumen automatico de correo. Dos disparadores:
#   - Lunes a viernes 8:06 (un minuto despues de abrir Gmail en Chrome).
#   - Al iniciar sesion en Windows (+2 min para que haya internet), cualquier dia.
# automatizacion\resumen-correo.ps1 lo hace una sola vez al dia: gana el primer disparador.
# Ejecuta Claude sin ventana y la pestaña Jarvis lee la lista en voz alta.
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre  = "Resumen de correo vacantes"
$hora    = "8:06AM"
$dias    = "Monday","Tuesday","Wednesday","Thursday","Friday"
$script  = (Resolve-Path (Join-Path $PSScriptRoot "..\automatizacion\resumen-correo.ps1")).Path
$usuario = "$env:USERDOMAIN\$env:USERNAME"

$action   = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$script`""
$t1 = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $dias -At $hora
$t2 = New-ScheduledTaskTrigger -AtLogOn -User $usuario
$t2.Delay = "PT2M"
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 10)

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger @($t1, $t2) -Settings $settings `
    -Description "Claude revisa Gmail y avisa las novedades de vacantes por voz: L-V 8:06 y al iniciar sesion (una vez al dia)" -Force | Out-Null

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State

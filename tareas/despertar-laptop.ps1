# Despierta la laptop de lunes a viernes a las 8:00 AM (desde suspension o hibernacion).
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre = "Despertar laptop 8AM"
$hora   = "8:00AM"
$dias   = "Monday","Tuesday","Wednesday","Thursday","Friday"

$action   = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c exit"
$trigger  = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $dias -At $hora
$settings = New-ScheduledTaskSettingsSet -WakeToRun -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger $trigger -Settings $settings `
    -Description "Despierta el equipo de lunes a viernes a las $hora" -Force | Out-Null

# Permitir temporizadores de reactivacion (con corriente y con bateria)
powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
powercfg /setdcvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
powercfg /setactive SCHEME_CURRENT

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State, @{n='WakeToRun'; e={$_.Settings.WakeToRun}}

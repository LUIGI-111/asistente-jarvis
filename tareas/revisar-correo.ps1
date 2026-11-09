# Abre Gmail (luigiriosd@gmail.com) en Chrome: Recibidos y Spam, para revisar novedades de vacantes.
# Corre 5 minutos despues de despertar la laptop (lunes a viernes, 8:05) para dar tiempo a que Windows arranque y conecte a internet.
# Se puede ejecutar varias veces: -Force reemplaza la tarea si ya existe.

$nombre = "Revisar correo vacantes"
$hora   = "8:05AM"
$dias   = "Monday","Tuesday","Wednesday","Thursday","Friday"
$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$cuenta = "luigiriosd@gmail.com"

$urls = "https://mail.google.com/mail/?authuser=$cuenta#inbox",
        "https://mail.google.com/mail/?authuser=$cuenta#spam"

$action   = New-ScheduledTaskAction -Execute $chrome -Argument ($urls -join " ")
$trigger  = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $dias -At $hora
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable

Register-ScheduledTask -TaskName $nombre -Action $action -Trigger $trigger -Settings $settings `
    -Description "Abre Gmail (Recibidos y Spam) a las $hora de lunes a viernes" -Force | Out-Null

Get-ScheduledTask -TaskName $nombre | Select-Object TaskName, State

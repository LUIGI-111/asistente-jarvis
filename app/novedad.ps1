# Añade una novedad a estado\novedades.jsonl; la pestaña del asistente la muestra y la lee en voz alta.
# Uso: powershell -File app\novedad.ps1 -Titulo "Correo revisado" -Texto "2 respuestas de reclutadores" [-Nivel ok|alerta|info] [-Voz "texto alterno para leer"]
param(
    [Parameter(Mandatory)][string]$Titulo,
    [Parameter(Mandatory)][string]$Texto,
    [ValidateSet('info', 'ok', 'alerta')][string]$Nivel = 'info',
    [string]$Voz
)
# Permite listas en una sola línea de comando: los dos caracteres \n se convierten en salto de línea.
$Texto = $Texto -replace '\\n', "`n"
$archivo = Join-Path $PSScriptRoot '..\estado\novedades.jsonl'
New-Item -ItemType Directory -Force (Split-Path $archivo) | Out-Null
$obj = [ordered]@{ ts = (Get-Date).ToString('s'); titulo = $Titulo; texto = $Texto; nivel = $Nivel }
if ($Voz) { $obj.voz = $Voz }
$linea = ($obj | ConvertTo-Json -Compress) + "`n"
[System.IO.File]::AppendAllText($archivo, $linea, (New-Object System.Text.UTF8Encoding($false)))

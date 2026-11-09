# Ejecuta Claude sin ventana: revisa Gmail (Recibidos), ordena las novedades de vacantes por importancia
# y las manda a la pestaña Jarvis con app\novedad.ps1 (se leen en voz alta).
# Lo dispara la tarea programada "Resumen de correo vacantes" (tareas\resumen-correo.ps1).
# Registro de la ejecucion: estado\resumen-correo.log

param([switch]$Forzar)

$ErrorActionPreference = 'Continue'
$raiz   = Split-Path $PSScriptRoot
$log    = Join-Path $raiz 'estado\resumen-correo.log'
$novedad = Join-Path $raiz 'app\novedad.ps1'
$OutputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Set-Location $raiz

# Una sola vez al dia: la tarea tiene dos disparadores (8:06 y al iniciar sesion) y gana el primero.
# -Forzar ignora esta marca (para pruebas manuales).
$marca = Join-Path $raiz 'estado\ultimo-resumen.txt'
$hoy = Get-Date -Format 'yyyy-MM-dd'
if (-not $Forzar -and (Test-Path $marca) -and ((Get-Content $marca -Raw).Trim() -eq $hoy)) {
    "=== $(Get-Date -Format s) === omitido: el resumen de hoy ya se hizo" | Add-Content $log -Encoding UTF8
    exit 0
}

$prompt = Get-Content (Join-Path $PSScriptRoot 'prompt-resumen-correo.md') -Raw -Encoding UTF8
$herramientas = @(
    'mcp__claude_ai_Gmail__search_threads',
    'mcp__claude_ai_Gmail__get_thread',
    'Bash(powershell*novedad.ps1*)'
)

"=== $(Get-Date -Format s) ===" | Add-Content $log -Encoding UTF8
$salida = $prompt | & claude -p --model sonnet --allowedTools $herramientas 2>&1
$codigo = $LASTEXITCODE
($salida | Out-String) | Add-Content $log -Encoding UTF8
"codigo de salida: $codigo" | Add-Content $log -Encoding UTF8

if ($codigo -eq 0) {
    Set-Content $marca $hoy -Encoding ASCII
}

if ($codigo -ne 0) {
    & $novedad -Titulo 'No pude revisar el correo' -Texto 'La revisión automática de Gmail falló. Revisa estado\resumen-correo.log.' `
        -Voz 'No pude revisar tu correo esta mañana.' -Nivel alerta
}
exit $codigo

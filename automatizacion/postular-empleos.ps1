# Ejecuta Claude sin ventana: busca vacantes nuevas en LinkedIn (última semana), filtra con
# CONTEXTO-BUSQUEDA-EMPLEO.md y envía las postulaciones que calzan. Avisa por la pestaña Jarvis.
# Lo dispara la tarea programada "Postulaciones de empleo" (tareas\postular-empleos.ps1).
# Requiere Chrome abierto con la extensión Claude in Chrome y LinkedIn con sesión iniciada.
# Registro de la ejecución: estado\postulaciones.log

param([switch]$Forzar)

$ErrorActionPreference = 'Continue'
$raiz    = Split-Path $PSScriptRoot
$log     = Join-Path $raiz 'estado\postulaciones.log'
$novedad = Join-Path $raiz 'app\novedad.ps1'
$OutputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Set-Location $raiz

# Una sola vez al día (la tarea tiene dos disparadores). -Forzar ignora la marca.
$marca = Join-Path $raiz 'estado\ultima-postulacion.txt'
$hoy = Get-Date -Format 'yyyy-MM-dd'
if (-not $Forzar -and (Test-Path $marca) -and ((Get-Content $marca -Raw).Trim() -eq $hoy)) {
    "=== $(Get-Date -Format s) === omitido: ya se postuló hoy" | Add-Content $log -Encoding UTF8
    exit 0
}

# Chrome tiene que estar abierto para que la extensión responda.
if (-not (Get-Process chrome -ErrorAction SilentlyContinue)) {
    Start-Process chrome 'https://www.linkedin.com/jobs/'
    Start-Sleep 20
}

$prompt = Get-Content (Join-Path $PSScriptRoot 'prompt-postulaciones.md') -Raw -Encoding UTF8
$herramientas = @(
    'mcp__claude-in-chrome',
    'Read', 'Edit',
    'Bash(powershell*novedad.ps1*)'
)

"=== $(Get-Date -Format s) ===" | Add-Content $log -Encoding UTF8
$salida = $prompt | & claude -p --chrome --model sonnet --allowedTools $herramientas 2>&1
$codigo = $LASTEXITCODE
($salida | Out-String) | Add-Content $log -Encoding UTF8
"codigo de salida: $codigo" | Add-Content $log -Encoding UTF8

if ($codigo -eq 0) {
    Set-Content $marca $hoy -Encoding ASCII
} else {
    & $novedad -Titulo 'No pude postular hoy' -Texto 'La búsqueda automática de empleo falló. Revisa estado\postulaciones.log.' `
        -Voz 'No pude enviar postulaciones hoy. Revisa el registro.' -Nivel alerta
}
exit $codigo

# Abre un chat de Claude Code en la carpeta ASISTENTE y le pide un resumen de donde quedo el proyecto.
# Lo lanza la tarea "Chat Claude ASISTENTE al iniciar sesion" (tareas\iniciar-chat-claude.ps1).
Set-Location (Split-Path $PSScriptRoot)
$Host.UI.RawUI.WindowTitle = 'Claude - ASISTENTE'
claude "Lee estado\ESTADO.md y dime en 3 líneas dónde quedamos y qué sigue."

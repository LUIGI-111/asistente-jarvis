# ASISTENTE

Asistente personal de LUIGI-111. Cada tarea es un script de PowerShell en `tareas/` que crea una tarea programada de Windows. Se pueden ejecutar varias veces sin duplicar nada.

## Tareas

| Tarea | Script | Cuándo | Qué hace |
|-------|--------|--------|----------|
| Despertar laptop 8AM | `tareas/despertar-laptop.ps1` | Lunes a viernes, 8:00 | Despierta la laptop desde suspensión o hibernación. No enciende un equipo apagado por completo. |
| Revisar correo vacantes | `tareas/revisar-correo.ps1` | Lunes a viernes, 8:05 | Abre Chrome con Gmail (luigiriosd@gmail.com) en dos pestañas: Recibidos y Spam, para ver novedades de las vacantes enviadas. |
| Resumen de correo vacantes | `tareas/resumen-correo.ps1` | Lunes a viernes 8:06 y al iniciar sesión (+2 min); una vez al día | Ejecuta Claude sin ventana (`automatizacion/resumen-correo.ps1`): revisa Recibidos, ordena las novedades de vacantes por importancia y la pestaña Jarvis las lee en voz alta como lista. Registro en `estado/resumen-correo.log`. |
| Chat Claude ASISTENTE al iniciar sesion | `tareas/iniciar-chat-claude.ps1` | Al iniciar sesión (+30 s) | Abre una ventana de PowerShell con un chat de Claude Code en esta carpeta, que lee `estado/ESTADO.md` y resume dónde quedamos. Desactivar: `Disable-ScheduledTask -TaskName "Chat Claude ASISTENTE al iniciar sesion"`. |
| Pestana Jarvis al iniciar sesion | `tareas/iniciar-pestana.ps1` | Al iniciar sesión (+15 s) | Abre la pestaña flotante Jarvis (`app\iniciar.bat`). |

## Pestaña Jarvis

Ventana flotante con orbe animado que muestra y lee en voz alta las novedades del asistente. Código y uso en `app/README.md`; el estado del proyecto para traspasar entre chats está en `estado/ESTADO.md` (reglas para Claude en `CLAUDE.md`). Se abre con `app\iniciar.bat`.

## Cómo recrear todo en otro equipo

```powershell
powershell -ExecutionPolicy Bypass -File .\tareas\despertar-laptop.ps1
powershell -ExecutionPolicy Bypass -File .\tareas\revisar-correo.ps1
powershell -ExecutionPolicy Bypass -File .\tareas\resumen-correo.ps1
powershell -ExecutionPolicy Bypass -File .\tareas\iniciar-pestana.ps1
powershell -ExecutionPolicy Bypass -File .\tareas\iniciar-chat-claude.ps1
```

La pestaña Jarvis necesita además el JDK portable en `app\.tools` (JDK 21 Liberica Full con JavaFX) y compilar con `app\compilar.ps1`.

Las tareas se ven con `taskschd.msc`.

## Notas

- Equipo: laptop HP Victus, Windows 11.
- Para despertar bien, conviene dejarla enchufada y con la tapa en acción "Suspender".

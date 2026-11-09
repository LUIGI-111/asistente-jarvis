# CLAUDE.md — ASISTENTE

Proyecto: asistente personal de LUIGI-111 (Windows 11, laptop HP Victus). Respóndele siempre en español.

## Al empezar un chat nuevo

1. Lee `estado/ESTADO.md`: es el resumen vivo del proyecto (qué existe, qué falta, decisiones).
2. Lee `README.md`: índice de tareas programadas.

## Reglas obligatorias al trabajar aquí

Cada vez que termines algo relevante (crear/cambiar una tarea, tomar una decisión, resolver un problema, dejar algo pendiente):

1. **Actualiza `estado/ESTADO.md`** para que otro chat pueda retomar sin contexto.
2. **Avisa a la pestaña flotante** ejecutando:
   ```powershell
   powershell -ExecutionPolicy Bypass -File "C:\Users\yosoy\OneDrive\Escritorio\ASISTENTE\app\novedad.ps1" -Titulo "Título corto" -Texto "Qué pasó, en una o dos frases" -Nivel info
   ```
   `-Nivel` puede ser `info`, `ok` o `alerta`. La app lo muestra y lo **lee en voz alta**, así que escribe frases cortas y naturales, sin markdown ni rutas largas. Usa `-Voz "texto"` si lo que se debe leer difiere de lo que se muestra.
3. Para cada tarea programada nueva: script en `tareas/` (idempotente, `-Force`), ejecutarlo y añadir fila al `README.md`.

## Estructura

| Ruta | Qué es |
|------|--------|
| `tareas/` | Scripts `.ps1` que registran tareas programadas de Windows |
| `estado/ESTADO.md` | Resumen vivo para traspasar contexto entre chats |
| `estado/novedades.jsonl` | Bitácora: una línea JSON por novedad (`ts`, `titulo`, `texto`, `nivel`, `voz` opcional). La lee la app |
| `automatizacion/` | Lo que se ejecuta sin ventana desde tareas programadas (resumen de correo con `claude -p` y su prompt) |
| `app/` | Pestaña flotante en Java 21 + JavaFX (ver `app/README.md`) |

No edites `novedades.jsonl` a mano con otro formato: usa `app/novedad.ps1`.

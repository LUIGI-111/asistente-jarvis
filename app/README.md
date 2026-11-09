# app — Pestaña Jarvis

Ventana flotante (Java 21 + JavaFX) que muestra y lee en voz alta las novedades de `../estado/novedades.jsonl`.

## Uso

```powershell
powershell -File .\compilar.ps1   # solo si cambias el código en src\
.\iniciar.bat                     # abre la pestaña
```

Enviar una novedad (también lo hacen otros chats de Claude):

```powershell
powershell -File .\novedad.ps1 -Titulo "Título" -Texto "Qué pasó" -Nivel ok
```

## Formato de `novedades.jsonl`

Una línea JSON por novedad: `{"ts":"2026-11-03T08:05:00","titulo":"...","texto":"...","nivel":"info|ok|alerta","voz":"opcional"}`.
Una línea de texto plano también se acepta y se muestra como "Novedad".

## Piezas

- `src/jarvis/Main.java`: ventana, tarjetas y botones.
- `src/jarvis/OrbView.java`: animación del orbe (reposo vs. hablando).
- `src/jarvis/Voz.java`: cola de voz con System.Speech (PowerShell).
- `src/jarvis/Estado.java`: vigila el archivo por sondeo y lo interpreta.
- `.tools/`: JDK 21 portable con JavaFX y Maven (se puede borrar y volver a descargar; no se usa Maven).

Para usar otro archivo de novedades: `-Djarvis.estado=RUTA` en `iniciar.bat`.

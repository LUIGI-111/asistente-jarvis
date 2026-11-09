package jarvis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Lee texto en voz alta con la voz de Windows (System.Speech), en cola y sin solaparse. */
final class Voz {

    // Sin comillas dobles a propósito: así Java las pasa a PowerShell sin problemas de escape.
    private static final String SCRIPT =
            "Add-Type -AssemblyName System.Speech; "
            + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
            + "$v = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq 'es' } | Select-Object -First 1; "
            + "if ($v) { $s.SelectVoice($v.VoiceInfo.Name) }; "
            + "$s.Speak($env:JARVIS_TEXTO)";

    private final ExecutorService cola = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "voz");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger pendientes = new AtomicInteger();
    private final Consumer<Boolean> alCambiar;
    private volatile Process actual;
    private volatile boolean silenciado;

    Voz(Consumer<Boolean> alCambiar) {
        this.alCambiar = alCambiar;
    }

    boolean isSilenciado() {
        return silenciado;
    }

    void setSilenciado(boolean valor) {
        this.silenciado = valor;
        Process p = actual;
        if (valor && p != null) {
            p.destroy();
        }
    }

    void decir(String texto) {
        if (silenciado || texto == null) {
            return;
        }
        String limpio = texto.replaceAll("[`*#_|]", "").replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return;
        }
        if (limpio.length() > 1500) {
            limpio = limpio.substring(0, 1500);
        }
        final String hablar = limpio;
        pendientes.incrementAndGet();
        cola.submit(() -> {
            try {
                if (silenciado) {
                    return;
                }
                alCambiar.accept(true);
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                        "-WindowStyle", "Hidden", "-Command", SCRIPT);
                pb.environment().put("JARVIS_TEXTO", hablar);
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process p = pb.start();
                actual = p;
                p.waitFor();
            } catch (Exception ignorada) {
                // si la voz falla, la app sigue mostrando el texto
            } finally {
                actual = null;
                if (pendientes.decrementAndGet() == 0) {
                    alCambiar.accept(false);
                }
            }
        });
    }

    void cerrar() {
        Process p = actual;
        if (p != null) {
            p.destroy();
        }
        cola.shutdownNow();
    }
}

package jarvis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversa con Claude Code en segundo plano. Mantiene UN proceso claude abierto (stream-json) para no
 * pagar el arranque en cada mensaje; solo se reinicia al pasar de modo normal a modo "confirmado".
 */
final class ChatClaude {

    /** Respuesta ya separada: texto para mostrar, resumen para la voz y si pide confirmación. */
    record Respuesta(String texto, String voz, boolean pideConfirmacion, boolean error) { }

    private static final List<String> PERMITIDAS = List.of(
            "Read", "Glob", "Grep", "Edit", "Write", "Bash", "PowerShell", "WebSearch", "WebFetch",
            "mcp__claude_ai_Gmail__search_threads", "mcp__claude_ai_Gmail__get_thread",
            "mcp__claude_ai_Gmail__get_message", "mcp__claude_ai_Gmail__list_labels");

    /** Extra que se permite solo en el turno que el usuario confirmó. */
    private static final List<String> CONFIRMADAS = List.of(
            "Bash(rm *)", "Bash(del *)", "Bash(mv *)", "Bash(move *)", "Bash(*Remove-Item*)", "Bash(*Move-Item*)",
            "PowerShell(*Remove-Item*)", "PowerShell(*Move-Item*)",
            "mcp__claude_ai_Gmail__send_message", "mcp__claude_ai_Gmail__reply", "mcp__claude_ai_Gmail__forward",
            "mcp__claude_ai_Gmail__create_draft");

    /** Acciones delicadas: bloqueadas salvo que el usuario haya confirmado en el chat. */
    private static final List<String> BLOQUEADAS = List.of(
            "Bash(rm *)", "Bash(del *)", "Bash(rd *)", "Bash(rmdir *)", "Bash(mv *)", "Bash(move *)",
            "Bash(git push*)", "Bash(npm install*)", "Bash(pip install*)", "Bash(winget *)", "Bash(choco *)",
            "Bash(*Remove-Item*)", "Bash(*Move-Item*)", "Bash(*Install-*)", "Bash(*Stop-Process*)",
            "Bash(*Send-MailMessage*)", "Bash(*reg add*)", "Bash(*reg delete*)", "Bash(*Set-ExecutionPolicy*)",
            "PowerShell(*Remove-Item*)", "PowerShell(*Move-Item*)", "PowerShell(*Install-*)",
            "PowerShell(*Stop-Process*)", "PowerShell(*Send-MailMessage*)", "PowerShell(*Set-ExecutionPolicy*)",
            "mcp__claude_ai_Gmail__send_message", "mcp__claude_ai_Gmail__reply", "mcp__claude_ai_Gmail__forward",
            "mcp__claude_ai_Gmail__trash_message", "mcp__claude_ai_Gmail__trash_thread",
            "mcp__claude_ai_Gmail__delete_draft", "mcp__claude_ai_Gmail__delete_label");

    private static final Pattern RESULTADO = Pattern.compile("\"result\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final ExecutorService cola = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-claude");
        t.setDaemon(true);
        return t;
    });
    private final Path carpeta;       // carpeta ASISTENTE (directorio de trabajo)
    private final Path promptSistema;
    private final String sesion = UUID.randomUUID().toString();
    private boolean sesionIniciada;
    private boolean confirmacionPendiente;

    private Process proceso;
    private OutputStream entrada;
    private boolean procesoConfirmado;
    private volatile Consumer<Respuesta> pendiente;

    ChatClaude(Path carpeta) {
        this.carpeta = carpeta;
        this.promptSistema = carpeta.resolve("automatizacion").resolve("prompt-chat.md");
        cola.submit(() -> {
            try {
                arrancar(false); // se calienta al abrir la app, antes del primer mensaje
            } catch (IOException ignorada) {
                // se reintentará al enviar el primer mensaje
            }
        });
    }

    void enviar(String mensaje, Consumer<Respuesta> alTerminar) {
        cola.submit(() -> ejecutar(mensaje, alTerminar));
    }

    private void ejecutar(String mensaje, Consumer<Respuesta> alTerminar) {
        // Si la respuesta anterior pidió confirmar, este mensaje es la decisión.
        boolean confirmado = confirmacionPendiente;
        try {
            if (proceso == null || !proceso.isAlive() || procesoConfirmado != confirmado) {
                arrancar(confirmado);
            }
            pendiente = r -> {
                if (!r.error()) {
                    sesionIniciada = true;
                    confirmacionPendiente = r.pideConfirmacion();
                }
                alTerminar.accept(r);
            };
            String json = "{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":\""
                    + escapar(mensaje) + "\"}}\n";
            entrada.write(json.getBytes(StandardCharsets.UTF_8));
            entrada.flush();
        } catch (IOException e) {
            pendiente = null;
            alTerminar.accept(new Respuesta("No pude lanzar Claude: " + e.getMessage(),
                    "No pude lanzar Claude.", false, true));
        }
    }

    private void arrancar(boolean confirmado) throws IOException {
        detener();
        List<String> cmd = new ArrayList<>();
        cmd.add(ejecutable());
        cmd.add("-p");
        cmd.add("--input-format");
        cmd.add("stream-json");
        cmd.add("--output-format");
        cmd.add("stream-json");
        cmd.add("--verbose");
        cmd.add(sesionIniciada ? "--resume" : "--session-id");
        cmd.add(sesion);
        cmd.add("--model");
        cmd.add("sonnet");
        cmd.add("--effort");
        cmd.add("low");
        cmd.add("--disable-slash-commands");
        cmd.add("--append-system-prompt-file");
        cmd.add(promptSistema.toString());
        cmd.add("--allowedTools");
        cmd.addAll(PERMITIDAS);
        if (confirmado) {
            // Tras el "sí" del usuario se permiten más herramientas, pero sin saltarse los permisos.
            cmd.addAll(CONFIRMADAS);
            cmd.add("--permission-mode");
            cmd.add("acceptEdits");
        } else {
            cmd.add("--disallowedTools");
            cmd.addAll(BLOQUEADAS);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(carpeta.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        proceso = p;
        entrada = p.getOutputStream();
        procesoConfirmado = confirmado;
        Thread lector = new Thread(() -> leer(p), "chat-claude-lector");
        lector.setDaemon(true);
        lector.start();
    }

    /** Lee la salida línea a línea; cada línea "result" es una respuesta completa. */
    private void leer(Process p) {
        StringBuilder basura = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.startsWith("{") && linea.contains("\"type\":\"result\"")) {
                    Consumer<Respuesta> cb = pendiente;
                    pendiente = null;
                    if (cb != null) {
                        boolean error = linea.contains("\"is_error\":true");
                        Matcher m = RESULTADO.matcher(linea);
                        String texto = m.find() ? desescapar(m.group(1)) : "";
                        cb.accept(error ? new Respuesta(texto.isBlank() ? "Claude devolvió un error." : texto,
                                "No pude responder.", false, true) : interpretar(texto));
                    }
                } else if (!linea.startsWith("{")) {
                    basura.append(linea).append('\n');
                }
            }
        } catch (IOException ignorada) {
            // el proceso se cerró
        }
        Consumer<Respuesta> cb = pendiente;
        if (cb != null && p == proceso) { // murió a mitad de una respuesta
            pendiente = null;
            String msg = basura.toString().strip();
            cb.accept(new Respuesta(msg.isEmpty() ? "Claude se cerró sin responder." : msg,
                    "No pude responder.", false, true));
        }
    }

    /** Separa las marcas [VOZ] y [CONFIRMAR] del texto que se muestra. */
    static Respuesta interpretar(String salida) {
        String voz = null;
        boolean confirmar = false;
        StringBuilder texto = new StringBuilder();
        for (String linea : salida.split("\\R")) {
            String t = linea.strip();
            if (t.startsWith("[VOZ]")) {
                voz = t.substring(5).strip();
            } else if (t.equals("[CONFIRMAR]")) {
                confirmar = true;
            } else {
                texto.append(linea).append('\n');
            }
        }
        return new Respuesta(texto.toString().replace("**", "").strip(), voz, confirmar, false);
    }

    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                sb.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i + 4 < s.length()) {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> sb.append(n); // \" \\ \/
            }
        }
        return sb.toString();
    }

    private void detener() {
        Process p = proceso;
        proceso = null;
        if (p != null) {
            p.destroyForcibly();
        }
    }

    void cerrar() {
        detener();
        cola.shutdownNow();
    }

    /** claude.exe: primero la ruta de instalación habitual, y si no está, el PATH. */
    private static String ejecutable() {
        Path local = Path.of(System.getProperty("user.home"), ".local", "bin", "claude.exe");
        return Files.exists(local) ? local.toString() : "claude";
    }
}

package jarvis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vigila novedades.jsonl (una línea JSON por novedad) y avisa de las líneas nuevas.
 * Usa sondeo en vez de WatchService porque OneDrive no siempre dispara eventos de archivo.
 */
final class Estado {

    record Entrada(String hora, String titulo, String texto, String nivel, String voz) { }

    private static final Pattern CAMPO = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final Path archivo;
    private final Consumer<List<Entrada>> alHistorial;
    private final Consumer<List<Entrada>> alNuevas;
    private final ScheduledExecutorService reloj = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "estado");
        t.setDaemon(true);
        return t;
    });
    private int procesadas;
    private boolean primera = true;

    Estado(Path archivo, Consumer<List<Entrada>> alHistorial, Consumer<List<Entrada>> alNuevas) {
        this.archivo = archivo;
        this.alHistorial = alHistorial;
        this.alNuevas = alNuevas;
    }

    void iniciar() throws IOException {
        Files.createDirectories(archivo.getParent());
        if (!Files.exists(archivo)) {
            Files.createFile(archivo);
        }
        reloj.scheduleWithFixedDelay(this::sondear, 0, 700, TimeUnit.MILLISECONDS);
    }

    private void sondear() {
        try {
            String s = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
            if (s.startsWith("﻿")) {
                s = s.substring(1);
            }
            int ultimoSalto = s.lastIndexOf('\n');
            String cola = s.substring(ultimoSalto + 1).trim();
            // la última línea cuenta aunque no termine en salto de línea, si ya es un JSON completo
            String completo = cola.endsWith("}") ? s : (ultimoSalto < 0 ? "" : s.substring(0, ultimoSalto));
            String[] lineas = completo.isBlank() ? new String[0] : completo.split("\r?\n");
            if (lineas.length < procesadas) {
                procesadas = 0; // el archivo fue reescrito o vaciado
            }
            List<Entrada> nuevas = new ArrayList<>();
            for (int i = procesadas; i < lineas.length; i++) {
                Entrada e = interpretar(lineas[i]);
                if (e != null) {
                    nuevas.add(e);
                }
            }
            procesadas = lineas.length;
            if (primera) {
                primera = false;
                int desde = Math.max(0, nuevas.size() - 30);
                alHistorial.accept(new ArrayList<>(nuevas.subList(desde, nuevas.size())));
            } else if (!nuevas.isEmpty()) {
                alNuevas.accept(nuevas);
            }
        } catch (Exception ignorada) {
            // archivo bloqueado un instante por OneDrive o el escritor: se reintenta en el próximo ciclo
        }
    }

    static Entrada interpretar(String linea) {
        String l = linea.trim();
        if (l.isEmpty()) {
            return null;
        }
        Map<String, String> c = new HashMap<>();
        Matcher m = CAMPO.matcher(l);
        while (m.find()) {
            c.put(m.group(1), desescapar(m.group(2)));
        }
        String titulo = c.getOrDefault("titulo", "");
        String texto = c.getOrDefault("texto", "");
        if (titulo.isBlank() && texto.isBlank()) {
            if (l.startsWith("{")) {
                return null;
            }
            texto = l; // línea de texto plano
        }
        if (titulo.isBlank()) {
            titulo = "Novedad";
        }
        return new Entrada(hora(c.get("ts")), titulo, texto, c.getOrDefault("nivel", "info"), c.get("voz"));
    }

    private static String hora(String ts) {
        if (ts == null || ts.isBlank()) {
            return LocalTime.now().format(HORA);
        }
        int t = ts.indexOf('T');
        String parte = t >= 0 ? ts.substring(t + 1) : ts;
        return parte.length() >= 5 ? parte.substring(0, 5) : parte;
    }

    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 >= s.length()) {
                sb.append(ch);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> { }
                case 'u' -> {
                    if (i + 4 < s.length()) {
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            sb.append("\\u");
                        }
                    }
                }
                default -> sb.append(n); // \" \\ \/
            }
        }
        return sb.toString();
    }
}

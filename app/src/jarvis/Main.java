package jarvis;

import java.nio.file.Path;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** Pestaña flotante estilo Jarvis que muestra y lee en voz alta las novedades del asistente. */
public class Main extends Application {

    private static final int MAX_TARJETAS = 50;
    private static final String CIAN = "#22e5ff";

    private final VBox lista = new VBox(8);
    private final Label estado = new Label("EN LÍNEA");
    private OrbView orb;
    private Voz voz;
    private VBox ultima;
    private boolean hablando;
    private double dx, dy;

    private static final double ANCHO_CHAT = 380;
    private final VBox mensajes = new VBox(8);
    private final ScrollPane scrollChat = new ScrollPane(mensajes);
    private final TextField entrada = new TextField();
    private ChatClaude chat;
    private Stage ventana;
    private ScrollPane scrollNovedades;
    private VBox panelChat;
    private boolean pensando;
    private double anchoNormal;

    @Override
    public void start(Stage stage) throws Exception {
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        double ancho = Math.max(230, pantalla.getWidth() * 0.10);
        double alto = pantalla.getHeight() * 0.78;
        anchoNormal = ancho;
        ventana = stage;
        chat = new ChatClaude(Path.of(System.getProperty("jarvis.carpeta", ".."))
                .toAbsolutePath().normalize());

        orb = new OrbView(ancho - 24);
        voz = new Voz(h -> Platform.runLater(() -> {
            hablando = h;
            orb.setHablando(h);
            refrescarEstado();
        }));

        // barra superior (arrastrable)
        Label titulo = new Label("ASISTENTE");
        titulo.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        titulo.setTextFill(Color.web(CIAN));
        Region relleno = new Region();
        HBox.setHgrow(relleno, Priority.ALWAYS);
        Button btnVoz = boton("VOZ");
        Button btnChat = boton("CHAT");
        Button btnCerrar = boton("✕");
        btnChat.setOnAction(e -> alternarChat(btnChat));
        btnVoz.setOnAction(e -> {
            voz.setSilenciado(!voz.isSilenciado());
            btnVoz.setOpacity(voz.isSilenciado() ? 0.4 : 1.0);
            refrescarEstado();
        });
        btnCerrar.setOnAction(e -> cerrar());
        HBox barra = new HBox(6, titulo, relleno, btnChat, btnVoz, btnCerrar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setOnMousePressed(e -> {
            dx = e.getScreenX() - stage.getX();
            dy = e.getScreenY() - stage.getY();
        });
        barra.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dx);
            stage.setY(e.getScreenY() - dy);
        });

        estado.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        estado.setTextFill(Color.web("#5eead4"));
        estado.setMaxWidth(Double.MAX_VALUE);
        estado.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(lista);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        lista.setStyle("-fx-background-color: transparent;");

        scrollNovedades = scroll;
        construirPanelChat();
        StackPane centro = new StackPane(scroll, panelChat);
        VBox raiz = new VBox(8, barra, orb, estado, centro);
        VBox.setVgrow(centro, Priority.ALWAYS);
        raiz.setPadding(new Insets(10));
        raiz.setAlignment(Pos.TOP_CENTER);
        raiz.setStyle("-fx-background-color: rgba(4,12,20,0.93); -fx-background-radius: 14;"
                + "-fx-border-color: #0e7490; -fx-border-radius: 14; -fx-border-width: 1;");

        Scene escena = new Scene(raiz, ancho, alto);
        escena.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(escena);
        stage.setX(pantalla.getMaxX() - ancho - 8);
        stage.setY(pantalla.getMinY() + 40);
        stage.setTitle("Asistente");
        stage.show();

        Path archivo = Path.of(System.getProperty("jarvis.estado", "../estado/novedades.jsonl"))
                .toAbsolutePath().normalize();
        Estado vigilante = new Estado(archivo,
                historial -> Platform.runLater(() -> {
                    for (Estado.Entrada e : historial) {
                        agregar(e, false);
                    }
                    if (historial.isEmpty()) {
                        agregar(new Estado.Entrada("--:--", "Sistema en línea",
                                "Esperando novedades en " + archivo.getFileName(), "info", null), false);
                    }
                    voz.decir("Asistente en línea");
                }),
                nuevas -> Platform.runLater(() -> {
                    for (Estado.Entrada e : nuevas) {
                        agregar(e, true);
                        voz.decir(e.voz() != null && !e.voz().isBlank() ? e.voz() : e.titulo() + ". " + e.texto());
                    }
                }));
        vigilante.iniciar();
    }

    private Button boton(String texto) {
        Button b = new Button(texto);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #5eead4; -fx-border-color: #155e75;"
                + "-fx-border-radius: 4; -fx-font-size: 10; -fx-padding: 1 6; -fx-cursor: hand;");
        return b;
    }

    private void construirPanelChat() {
        mensajes.setStyle("-fx-background-color: transparent;");
        scrollChat.setFitToWidth(true);
        scrollChat.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollChat.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollChat.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        mensajes.heightProperty().addListener((o, a, b) -> scrollChat.setVvalue(1.0));

        entrada.setPromptText("Escribe y pulsa Enter…");
        entrada.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #e6f6fa;"
                + "-fx-prompt-text-fill: #5b7f8a; -fx-border-color: #155e75; -fx-border-radius: 6;"
                + "-fx-background-radius: 6; -fx-font-size: 12;");
        HBox.setHgrow(entrada, Priority.ALWAYS);
        Button enviar = boton("ENVIAR");
        entrada.setOnAction(e -> enviarMensaje());
        enviar.setOnAction(e -> enviarMensaje());

        HBox fila = new HBox(6, entrada, enviar);
        fila.setAlignment(Pos.CENTER_LEFT);
        panelChat = new VBox(8, scrollChat, fila);
        VBox.setVgrow(scrollChat, Priority.ALWAYS);
        panelChat.setVisible(false);
        panelChat.setManaged(false);
    }

    private void alternarChat(Button btn) {
        boolean abrir = !panelChat.isVisible();
        double derecha = ventana.getX() + ventana.getWidth();
        double nuevo = abrir ? ANCHO_CHAT : anchoNormal;
        panelChat.setVisible(abrir);
        panelChat.setManaged(abrir);
        scrollNovedades.setVisible(!abrir);
        scrollNovedades.setManaged(!abrir);
        btn.setOpacity(abrir ? 0.6 : 1.0);
        ventana.setWidth(nuevo);
        ventana.setX(derecha - nuevo);
        if (abrir) {
            if (mensajes.getChildren().isEmpty()) {
                burbuja("Hola. Escríbeme lo que necesites; antes de hacer algo delicado te pregunto.", false);
            }
            entrada.requestFocus();
        }
    }

    private void enviarMensaje() {
        String texto = entrada.getText().strip();
        if (texto.isEmpty() || pensando) {
            return;
        }
        entrada.clear();
        burbuja(texto, true);
        pensando = true;
        entrada.setDisable(true);
        refrescarEstado();
        chat.enviar(texto, r -> Platform.runLater(() -> {
            pensando = false;
            entrada.setDisable(false);
            entrada.requestFocus();
            burbuja(r.texto(), false, r.error());
            if (r.pideConfirmacion()) {
                burbuja("Responde sí para continuar o no para cancelar.", false);
            }
            if (r.voz() != null && !r.voz().isBlank()) {
                voz.decir(r.voz());
            }
            refrescarEstado();
        }));
    }

    private void burbuja(String texto, boolean mio) {
        burbuja(texto, mio, false);
    }

    private void burbuja(String texto, boolean mio, boolean error) {
        Label l = new Label(texto);
        l.setFont(Font.font("Segoe UI", 12));
        l.setWrapText(true);
        l.setMaxWidth(ANCHO_CHAT - 70);
        l.setTextFill(Color.web(error ? "#fdba74" : "#e6f6fa"));
        l.setPadding(new Insets(6, 9, 6, 9));
        l.setStyle("-fx-background-radius: 8; -fx-background-color: "
                + (mio ? "rgba(34,229,255,0.16)" : "rgba(255,255,255,0.06)") + ";");
        HBox fila = new HBox(l);
        fila.setAlignment(mio ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        mensajes.getChildren().add(fila);
    }

    private void refrescarEstado() {
        if (pensando) {
            estado.setText("PENSANDO…");
        } else if (voz.isSilenciado()) {
            estado.setText("SILENCIADO");
        } else if (hablando) {
            estado.setText("HABLANDO…");
        } else {
            estado.setText("EN LÍNEA");
        }
    }

    private static String acento(String nivel) {
        return switch (nivel == null ? "" : nivel.toLowerCase()) {
            case "alerta", "error" -> "#fb923c";
            case "ok", "exito", "éxito" -> "#4ade80";
            default -> CIAN;
        };
    }

    private void agregar(Estado.Entrada e, boolean nueva) {
        Label cab = new Label(e.hora() + "  " + e.titulo().toUpperCase());
        cab.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        cab.setTextFill(Color.web(acento(e.nivel())));
        cab.setWrapText(true);
        Label cuerpo = new Label(e.texto());
        cuerpo.setFont(Font.font("Segoe UI", 12));
        cuerpo.setTextFill(Color.web("#c3dde4"));
        cuerpo.setWrapText(true);

        VBox tarjeta = new VBox(3, cab, cuerpo);
        tarjeta.setPadding(new Insets(7, 8, 7, 8));
        tarjeta.setUserData(acento(e.nivel()));
        estilo(tarjeta, true);

        if (ultima != null) {
            estilo(ultima, false);
        }
        ultima = tarjeta;
        lista.getChildren().add(0, tarjeta);
        if (lista.getChildren().size() > MAX_TARJETAS) {
            lista.getChildren().remove(MAX_TARJETAS, lista.getChildren().size());
        }
        if (nueva) {
            FadeTransition ft = new FadeTransition(Duration.millis(450), tarjeta);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private static void estilo(VBox tarjeta, boolean reciente) {
        String acento = (String) tarjeta.getUserData();
        tarjeta.setStyle("-fx-background-color: " + (reciente ? "rgba(34,229,255,0.10)" : "rgba(255,255,255,0.03)") + ";"
                + "-fx-background-radius: 6; -fx-border-radius: 6;"
                + "-fx-border-color: " + (reciente ? "#0e7490" : "#12303a") + " #12303a #12303a " + acento + ";"
                + "-fx-border-width: 1 1 1 3;");
    }

    private void cerrar() {
        voz.cerrar();
        chat.cerrar();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

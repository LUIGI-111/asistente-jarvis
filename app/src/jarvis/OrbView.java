package jarvis;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/** Orbe animado estilo Jarvis: respira en reposo y pulsa con fuerza mientras habla. */
final class OrbView extends Canvas {

    private final long t0 = System.nanoTime();
    private volatile boolean hablando;
    private double energia = 0.1;

    OrbView(double lado) {
        super(lado, lado);
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                dibujar((now - t0) / 1e9);
            }
        }.start();
    }

    void setHablando(boolean valor) {
        this.hablando = valor;
    }

    private static Color cian(double alfa) {
        return Color.color(0.0, 0.9, 1.0, Math.max(0, Math.min(1, alfa)));
    }

    private void dibujar(double t) {
        double objetivo = hablando
                ? 0.6 + 0.4 * Math.abs(Math.sin(t * 8.3) * Math.cos(t * 3.1) + 0.3 * Math.sin(t * 19))
                : 0.12 + 0.08 * Math.sin(t * 1.6);
        objetivo = Math.max(0, Math.min(1, objetivo));
        energia += (objetivo - energia) * 0.18;

        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth();
        double cx = w / 2, cy = w / 2, R = w / 2 - 4;
        g.clearRect(0, 0, w, w);

        // halo
        g.setFill(new RadialGradient(0, 0, cx, cy, R, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, cian(0.10 + 0.30 * energia)), new Stop(1.0, cian(0))));
        g.fillOval(cx - R, cy - R, 2 * R, 2 * R);

        // núcleo
        double cr = R * (0.30 + 0.10 * energia);
        g.setFill(new RadialGradient(0, 0, cx, cy, cr, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#e6ffff")),
                new Stop(0.45, cian(0.9)),
                new Stop(1.0, Color.color(0.0, 0.55, 0.8, 0))));
        g.fillOval(cx - cr, cy - cr, 2 * cr, 2 * cr);

        // anillos segmentados que giran
        g.setLineCap(StrokeLineCap.BUTT);
        double pulso = 1 + 0.04 * energia;
        anillo(g, cx, cy, R * 0.52 * pulso, 3, 2.2, t * 40, 0.65, 0.35 + 0.55 * energia);
        anillo(g, cx, cy, R * 0.68 * pulso, 5, 1.6, -t * 28, 0.50, 0.30 + 0.45 * energia);
        anillo(g, cx, cy, R * 0.84 * pulso, 8, 1.2, t * 16, 0.45, 0.25 + 0.40 * energia);

        // marcas del borde
        g.setLineWidth(1);
        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(i * 6 + t * 5);
            double largo = (i % 5 == 0) ? 0.07 : 0.035;
            double r1 = R * 0.95, r2 = R * (0.95 + largo);
            g.setStroke(cian(0.25 + 0.3 * energia));
            g.strokeLine(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1,
                    cx + Math.cos(a) * r2, cy + Math.sin(a) * r2);
        }
    }

    private void anillo(GraphicsContext g, double cx, double cy, double r, int segmentos,
                        double grosor, double giroGrados, double fraccion, double alfa) {
        g.setLineWidth(grosor);
        g.setStroke(cian(alfa));
        double paso = 360.0 / segmentos;
        for (int k = 0; k < segmentos; k++) {
            g.strokeArc(cx - r, cy - r, 2 * r, 2 * r, giroGrados + k * paso, paso * fraccion, ArcType.OPEN);
        }
    }
}

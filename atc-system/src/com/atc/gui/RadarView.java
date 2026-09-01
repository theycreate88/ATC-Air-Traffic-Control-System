package com.atc.gui;

import com.atc.engine.ATCEngine;
import com.atc.model.Flight;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Classic circular radar scope: range rings, a rotating sweep arm with a
 * fading trail, and a blip for every tracked flight (color-coded by state,
 * same palette as the tactical map). Flight positions are taken straight
 * from the engine's map coordinate space and normalized into the circle, so
 * the radar always reflects the exact same live data as the main map.
 */
public class RadarView extends VBox {

    // Must match the coordinate space flights actually move in (see MapView / ATCEngine).
    private static final double MAP_W = 700;
    private static final double MAP_H = 700;

    private final ATCEngine engine;
    private final Canvas canvas = new Canvas();
    private double sweepDegrees = 0;

    public RadarView(ATCEngine engine) {
        this.engine = engine;
        getStyleClass().add("glass-panel");
        setSpacing(8);
        setPadding(new Insets(4));

        Label title = new Label("RADAR SCOPE");
        title.getStyleClass().add("panel-title");

        StackPane canvasHolder = new StackPane(canvas);
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());

        getChildren().addAll(title, canvasHolder);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                sweepDegrees = (sweepDegrees + 1.4) % 360;
                render();
            }
        };
        timer.start();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 1 || h <= 1) return;

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, w, h);

        double cx = w / 2;
        double cy = h / 2;
        double radius = Math.min(w, h) / 2 - 10;
        if (radius <= 0) return;

        drawRings(g, cx, cy, radius);
        drawSweep(g, cx, cy, radius);
        drawBlips(g, cx, cy, radius);
    }

    private void drawRings(GraphicsContext g, double cx, double cy, double radius) {
        g.setStroke(Color.web("#30363d", 0.8));
        g.setLineWidth(1);
        for (int i = 1; i <= 4; i++) {
            double r = radius * i / 4.0;
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.strokeLine(cx - radius, cy, cx + radius, cy);
        g.strokeLine(cx, cy - radius, cx, cy + radius);
    }

    private void drawSweep(GraphicsContext g, double cx, double cy, double radius) {
        g.save();
        g.translate(cx, cy);
        g.rotate(sweepDegrees);

        // Fading trail behind the sweep line
        int trailSteps = 26;
        for (int i = 0; i < trailSteps; i++) {
            double angle = -i * 1.1;
            double alpha = 0.16 * (1.0 - (double) i / trailSteps);
            g.setStroke(Color.web("#58a6ff", alpha));
            g.setLineWidth(2.2);
            double rad = Math.toRadians(angle);
            g.strokeLine(0, 0, radius * Math.cos(rad), radius * Math.sin(rad));
        }

        // Bright leading edge
        g.setStroke(Color.web("#58a6ff", 0.9));
        g.setLineWidth(2);
        g.strokeLine(0, 0, radius, 0);
        g.restore();
    }

    private void drawBlips(GraphicsContext g, double cx, double cy, double radius) {
        g.setFont(Font.font("Share Tech Mono", 8));
        for (Flight f : engine.getFlights()) {
            if (Flight.STATUS_LANDED.equals(f.getStatus())) continue;

            double nx = (f.getCurrentX() / MAP_W - 0.5) * 2; // -1 .. 1
            double ny = (f.getCurrentY() / MAP_H - 0.5) * 2;
            double bx = cx + nx * radius * 0.92;
            double by = cy + ny * radius * 0.92;

            Color color = colorFor(f);
            boolean swept = isNearSweep(bx - cx, by - cy);

            // Soft glow halo
            g.setFill(color.deriveColor(0, 1.0, 1.0, swept ? 0.5 : 0.25));
            double haloSize = swept ? 16 : 12;
            g.fillOval(bx - haloSize / 2, by - haloSize / 2, haloSize, haloSize);

            // Blip core
            g.setFill(color);
            double coreSize = swept ? 7 : 5;
            g.fillOval(bx - coreSize / 2, by - coreSize / 2, coreSize, coreSize);

            g.setFill(Color.web("#8b949e"));
            g.fillText(f.getFlightId(), bx + 6, by + 3);
        }
    }

    /** True if the point's angle from center is close to the current sweep angle (radar "paint" flash). */
    private boolean isNearSweep(double dx, double dy) {
        if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) return false;
        double blipAngle = Math.toDegrees(Math.atan2(dy, dx));
        if (blipAngle < 0) blipAngle += 360;
        double diff = Math.abs(blipAngle - sweepDegrees);
        diff = Math.min(diff, 360 - diff);
        return diff < 10;
    }

    private Color colorFor(Flight f) {
        boolean recentConflict = System.currentTimeMillis() - f.getLastConflictAtMillis() < 3000;
        if (recentConflict) return Color.web("#d29922");
        switch (f.getStatus()) {
            case Flight.STATUS_EMERGENCY:
                return Color.web("#f85149");
            case Flight.STATUS_ACTIVE:
                return Color.web("#58a6ff");
            default:
                return Color.web("#8b949e");
        }
    }
}

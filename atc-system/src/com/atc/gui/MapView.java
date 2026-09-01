package com.atc.gui;

import com.atc.engine.ATCEngine;
import com.atc.graph.Edge;
import com.atc.model.Airport;
import com.atc.model.Flight;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Random;

/**
 * Tactical map: a JavaFX Canvas rendered as a glowing sci-fi command-center
 * radar display. Draws the background grid, air routes, airport nodes,
 * animated flight markers (colour-coded by state) and weather hazard
 * overlays for flights in a weather emergency.
 *
 * Camera: normally shows the whole map. When a flight declares an emergency
 * (wired via ATCEngine.setOnEmergencyDeclared -> focusOnFlight), the camera
 * smoothly zooms in and follows that flight until it lands or the person
 * clicks anywhere on the map, at which point it eases back out to the full
 * overview. The engine also speeds up emergency flights, so the whole
 * zoomed-in diversion/landing plays out quickly and visibly.
 *
 * An AnimationTimer drives the simulation tick + redraw every frame, always
 * on the JavaFX Application Thread, so no extra synchronization is needed
 * here.
 */
public class MapView extends StackPane {

    private static final double CANVAS_W = 700;
    private static final double CANVAS_H = 700;

    private static final Color CYAN = Color.web("#58a6ff");
    private static final Color ORANGE = Color.web("#d29922");
    private static final Color RED = Color.web("#f85149");
    private static final Color GREEN = Color.web("#56d364");
    private static final Color SILVER = Color.web("#c9d1d9");
    private static final Color STEEL = Color.web("#8b949e");
    private static final Color GRID = Color.web("#30363d", 0.35);
    private static final Color ROUTE = Color.web("#58a6ff", 0.22);

    private final ATCEngine engine;
    private final Canvas canvas = new Canvas(CANVAS_W, CANVAS_H);
    private long lastNanos = -1;

    // --- Emergency zoom/follow camera -----------------------------------
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double FOCUS_ZOOM = 2.6;
    private static final double CAMERA_EASE = 0.07; // higher = snappier, lower = smoother/slower
    private String focusedFlightId = null;
    private double cameraZoom = DEFAULT_ZOOM;
    private double cameraPanX = CANVAS_W / 2.0;
    private double cameraPanY = CANVAS_H / 2.0;

    public MapView(ATCEngine engine) {
        this.engine = engine;
        getChildren().add(canvas);
        setStyle("-fx-background-color: #0d1117;");

        // Any click on the map drops the current focus, easing the camera back out.
        canvas.setOnMouseClicked(e -> focusedFlightId = null);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double delta = (lastNanos < 0) ? 0.016 : Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
                lastNanos = now;
                engine.tick(delta);
                render();
            }
        };
        timer.start();
    }

    /** Called (e.g. via ATCEngine's emergency-declared hook) to zoom the camera onto a flight. */
    public void focusOnFlight(String flightId) {
        this.focusedFlightId = flightId;
    }

    private void updateCamera() {
        double targetZoom = DEFAULT_ZOOM;
        double targetPanX = CANVAS_W / 2.0;
        double targetPanY = CANVAS_H / 2.0;

        if (focusedFlightId != null) {
            Flight f = engine.findFlightById(focusedFlightId);
            if (f == null || Flight.STATUS_LANDED.equals(f.getStatus())) {
                // The emergency resolved (landed) -- release focus, camera eases back out.
                focusedFlightId = null;
            } else {
                targetZoom = FOCUS_ZOOM;
                targetPanX = f.getCurrentX();
                targetPanY = f.getCurrentY();
            }
        }

        cameraZoom += (targetZoom - cameraZoom) * CAMERA_EASE;
        cameraPanX += (targetPanX - cameraPanX) * CAMERA_EASE;
        cameraPanY += (targetPanY - cameraPanY) * CAMERA_EASE;
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setEffect(null);
        g.setTransform(1, 0, 0, 1, 0, 0); // reset any transform left over from the previous frame
        g.clearRect(0, 0, CANVAS_W, CANVAS_H);
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, CANVAS_W, CANVAS_H);

        updateCamera();

        g.save();
        g.translate(CANVAS_W / 2.0, CANVAS_H / 2.0);
        g.scale(cameraZoom, cameraZoom);
        g.translate(-cameraPanX, -cameraPanY);

        drawGrid(g);
        drawRoutes(g);
        drawAirports(g);

        for (Flight f : engine.getFlights()) {
            if (Flight.STATUS_LANDED.equals(f.getStatus())) continue;
            if (f.isHasWeatherEmergency()) {
                drawWeatherHazard(g, f);
            }
            drawFlight(g, f);
        }

        g.restore();
    }

    private void drawGrid(GraphicsContext g) {
        g.setStroke(GRID);
        g.setLineWidth(1);
        for (double x = 0; x <= CANVAS_W; x += 35) {
            g.strokeLine(x, 0, x, CANVAS_H);
        }
        for (double y = 0; y <= CANVAS_H; y += 35) {
            g.strokeLine(0, y, CANVAS_W, y);
        }
    }

    private void drawRoutes(GraphicsContext g) {
        g.setStroke(ROUTE);
        g.setLineWidth(1.2);
        for (Airport a : engine.getAirports()) {
            for (Edge e : engine.getGraph().getEdges(a)) {
                Airport b = e.getTo();
                g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
        }
    }

    private void drawAirports(GraphicsContext g) {
        DropShadow glow = new DropShadow(BlurType.GAUSSIAN, CYAN, 10, 0.4, 0, 0);
        g.setFont(Font.font("Share Tech Mono", FontWeight.BOLD, 12));
        for (Airport a : engine.getAirports()) {
            g.setEffect(glow);
            g.setFill(CYAN);
            g.fillOval(a.getX() - 5, a.getY() - 5, 10, 10);
            g.setEffect(null);
            g.setFill(Color.web("#0d1117"));
            g.fillOval(a.getX() - 2, a.getY() - 2, 4, 4);

            g.setFill(SILVER);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(a.getName().toUpperCase(), a.getX() + 10, a.getY());
        }
    }

    private void drawFlight(GraphicsContext g, Flight f) {
        double x = f.getCurrentX();
        double y = f.getCurrentY();
        double angle = travelAngle(f);

        Color color = colorFor(f);

        DropShadow glow = new DropShadow(BlurType.GAUSSIAN, color, 8, 0.45, 0, 0);
        g.save();
        g.translate(x, y);
        g.rotate(Math.toDegrees(angle));
        g.setEffect(glow);
        drawAirplaneIcon(g, color);
        g.setEffect(null);
        g.restore();

        g.setFont(Font.font("Share Tech Mono", 9.5));
        g.setFill(STEEL);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.BOTTOM);
        String label = f.getFlightId() + " \u00B7 " + f.getAltitude() + "ft";
        g.fillText(label, x, y - 10);
    }

    /**
     * Draws a top-down airplane silhouette (nose along +x, matching the same
     * heading convention the old triangle used) -- fuselage, swept main
     * wings, smaller tail wings, and a tail fin -- instead of a plain
     * triangle marker. Assumes the context has already been translated to
     * the flight's position and rotated to its heading.
     */
    private void drawAirplaneIcon(GraphicsContext g, Color color) {
        g.save();
        g.scale(0.55, 0.55); // shape is authored at a larger, easier-to-read scale, then shrunk to fit the map

        g.beginPath();
        g.moveTo(18, 0);      // nose
        g.lineTo(10, -2);
        g.lineTo(10, -1);
        g.lineTo(2, -1);
        g.lineTo(2, -10);     // main wing tip
        g.lineTo(-1, -10);
        g.lineTo(-1, -2);
        g.lineTo(-9, -2);
        g.lineTo(-9, -5);     // tail wing tip
        g.lineTo(-11, -5);
        g.lineTo(-11, -2);
        g.lineTo(-14, -1);    // tail fin edge
        g.lineTo(-14, 1);
        g.lineTo(-11, 2);
        g.lineTo(-11, 5);     // tail wing tip (mirrored)
        g.lineTo(-9, 5);
        g.lineTo(-9, 2);
        g.lineTo(-1, 2);
        g.lineTo(-1, 10);     // main wing tip (mirrored)
        g.lineTo(2, 10);
        g.lineTo(2, 1);
        g.lineTo(10, 1);
        g.lineTo(10, 2);
        g.closePath();

        g.setFill(color);
        g.fill();
        g.setStroke(color.darker());
        g.setLineWidth(0.8);
        g.stroke();
        g.restore();
    }

    private Color colorFor(Flight f) {
        boolean recentConflict = System.currentTimeMillis() - f.getLastConflictAtMillis() < 3000;
        if (recentConflict) return ORANGE;
        switch (f.getStatus()) {
            case Flight.STATUS_EMERGENCY:
                return RED;
            case Flight.STATUS_ACTIVE:
                return CYAN;
            case Flight.STATUS_LANDED:
                return GREEN;
            default:
                return STEEL;
        }
    }

    private double travelAngle(Flight f) {
        Airport next = f.getNextAirport();
        if (next == null) return 0;
        double dx = next.getX() - f.getCurrentAirport().getX();
        double dy = next.getY() - f.getCurrentAirport().getY();
        return Math.atan2(dy, dx);
    }

    /**
     * Renders actual fluffy weather clouds along the flight's ENTIRE
     * remaining route (every leg still ahead of it, not just the one it's
     * currently flying), so the hazard reads as a real weather system
     * blanketing the flight path rather than a small marker-side effect.
     */
    private void drawWeatherHazard(GraphicsContext g, Flight f) {
        List<Airport> path = f.getRoutePath();
        int start = f.getCurrentRouteIndex();
        if (path == null || path.size() < 2 || start >= path.size() - 1) return;

        g.save();
        for (int i = start; i < path.size() - 1; i++) {
            double x1 = (i == start) ? f.getCurrentX() : path.get(i).getX();
            double y1 = (i == start) ? f.getCurrentY() : path.get(i).getY();
            double x2 = path.get(i + 1).getX();
            double y2 = path.get(i + 1).getY();
            drawCloudsAlongSegment(g, f.getFlightId(), i, x1, y1, x2, y2);
        }
        g.restore();
    }

    private void drawCloudsAlongSegment(GraphicsContext g, String flightId, int legIndex,
                                         double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-6) return;

        double perpX = -dy / length;
        double perpY = dx / length;

        int puffs = Math.max(2, (int) (length / 42));
        for (int i = 0; i <= puffs; i++) {
            // Deterministic per (flight, leg, puff) seed so the cloud formation holds
            // still frame-to-frame instead of flickering, while still varying puff to puff.
            long seed = flightId.hashCode() * 1_000_003L + legIndex * 97L + i;
            Random puffRand = new Random(seed);

            double t = (double) i / puffs;
            double cx = x1 + dx * t;
            double cy = y1 + dy * t;
            double lateralJitter = (puffRand.nextDouble() - 0.5) * 20;
            double scale = 0.75 + puffRand.nextDouble() * 0.55;
            drawCloud(g, cx + perpX * lateralJitter, cy + perpY * lateralJitter, scale);
        }
    }

    /** Draws one fluffy cloud (a cluster of overlapping soft ovals) centered at (cx, cy). */
    private void drawCloud(GraphicsContext g, double cx, double cy, double scale) {
        Color body = Color.web("#c9d1d9", 0.42);
        Color rim = Color.web("#d29922", 0.30);

        g.setFill(body);
        g.fillOval(cx - 22 * scale, cy - 7 * scale, 44 * scale, 18 * scale);
        g.fillOval(cx - 15 * scale, cy - 17 * scale, 22 * scale, 22 * scale);
        g.fillOval(cx - 1 * scale, cy - 21 * scale, 26 * scale, 24 * scale);
        g.fillOval(cx + 13 * scale, cy - 15 * scale, 20 * scale, 19 * scale);
        g.fillOval(cx - 27 * scale, cy - 2 * scale, 18 * scale, 16 * scale);
        g.fillOval(cx + 18 * scale, cy - 2 * scale, 16 * scale, 15 * scale);

        g.setStroke(rim);
        g.setLineWidth(1.1 * scale);
        g.strokeOval(cx - 22 * scale, cy - 7 * scale, 44 * scale, 18 * scale);
    }
}

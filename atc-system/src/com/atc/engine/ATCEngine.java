package com.atc.engine;

import com.atc.database.AirportDAO;
import com.atc.database.RouteDAO;
import com.atc.graph.Edge;
import com.atc.graph.FlightGraph;
import com.atc.model.Airport;
import com.atc.model.Flight;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;



public class ATCEngine {

    public static final double SAFE_LIMIT_PIXELS = 30.0; // minimum safe separation on screen
    // Lateral offset applied to each flight's rendered/simulated position, to the
    // right of
    // its travel direction. Two flights travelling the same route in opposite
    // directions end
    // up on opposite sides of the centerline, 2x this distance apart -- comfortably
    // more than
    // SAFE_LIMIT_PIXELS -- so head-on traffic never actually converges on the same
    // point.
    private static final double LANE_OFFSET_PIXELS = 20.0;

    private final FlightGraph graph = new FlightGraph();
    private final List<Airport> airports = new ArrayList<>();

    // HashMap: O(1) flight lookup by ID
    private final Map<String, Flight> flightRegistry = new HashMap<>();
    // Thread-safe list of every flight currently tracked by the system
    private final List<Flight> flights = new CopyOnWriteArrayList<>();

    private final Random rand = new Random();
    private int flightCounter = 1;

    private final ScheduledExecutorService collisionExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CollisionAvoidanceThread");
        t.setDaemon(true);
        return t;
    });

    private final List<String> eventLog = new CopyOnWriteArrayList<>();

    // Optional UI hook: fired synchronously (on whatever thread calls
    // createEmergency,
    // normally the JavaFX Application Thread) the instant a new emergency is
    // declared.
    private Consumer<Flight> onEmergencyDeclared;

    public void setOnEmergencyDeclared(Consumer<Flight> listener) {
        this.onEmergencyDeclared = listener;
    }

    public ATCEngine() {
        initializeAirports();
        startCollisionMonitor();
    }

    // -----------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------
    private void initializeAirports() {

    AirportDAO airportDAO = new AirportDAO();

    airportDAO.getAllAirports().forEach(a ->
            addAirport(
                    a.getName(),
                    a.getX(),
                    a.getY()
            )
    );

    RouteDAO routeDAO = new RouteDAO();

    routeDAO.getAllRoutes().forEach(r ->
            route(
                    r.getSource(),
                    r.getDestination(),
                    r.getDistance(),
                    r.getFlightTime()
            )
    );
}

    private void addAirport(String name, int x, int y) {
        Airport a = new Airport(name, x, y);
        airports.add(a);
        graph.addAirport(a);
    }

    private void route(String a, String b, double distance, double flightTimeMinutes) {
        Airport source = findAirport(a);
        Airport destination = findAirport(b);

        if (source == null || destination == null) {
            System.err.println("Skipping route because one or both airports were not found: " + a + " -> " + b);
            return;
        }

        graph.addRoute(source, destination, distance, flightTimeMinutes);
    }

    public Airport findAirport(String name) {
        for (Airport a : airports) {
            if (a.getName().equals(name))
                return a;
        }
        return null;
    }

    public List<Airport> getAirports() {
        return airports;
    }

    public FlightGraph getGraph() {
        return graph;
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    private void log(String msg) {
        eventLog.add(0, msg);
        while (eventLog.size() > 200) {
            eventLog.remove(eventLog.size() - 1);
        }
    }

    // -----------------------------------------------------------------
    // Flight generation
    // -----------------------------------------------------------------
    /** Generates a flight between two random, distinct airports. */
    public Flight generateFlight() {
        if (airports.size() < 2)
            return null;
        Airport dep = airports.get(rand.nextInt(airports.size()));
        Airport dest;
        do {
            dest = airports.get(rand.nextInt(airports.size()));
        } while (dest.equals(dep));
        return generateFlight(dep, dest);
    }

    /**
     * Generates a flight between the user-chosen departure and destination
     * airports (selected via dropdowns in the Control Panel). Returns null
     * if the airports are invalid, identical, or unreachable from each other.
     */
    public Flight generateFlight(Airport dep, Airport dest) {
        if (dep == null || dest == null || dep.equals(dest))
            return null;

        List<Airport> path = graph.dijkstra(dep, dest);
        if (path.isEmpty())
            return null;

        long now = System.currentTimeMillis();
        double totalMinutes = totalRouteMinutes(path);
        long eta = now + (long) (totalMinutes * 60_000);

        String id = "PK" + String.format("%03d", flightCounter++);
        Flight f = new Flight(id, dep, dest, path, now, eta);
        flightRegistry.put(id, f);
        flights.add(f);
        dep.requestDeparture(f);
        f.setStatus(Flight.STATUS_SCHEDULED);
        log("Flight " + id + " scheduled: " + dep.getName() + " -> " + dest.getName());
        return f;
    }

    private double totalRouteMinutes(List<Airport> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Edge e = edgeBetween(path.get(i), path.get(i + 1));
            if (e != null)
                total += e.getEstimatedFlightTime();
        }
        return total;
    }

    private Edge edgeBetween(Airport from, Airport to) {
        for (Edge e : graph.getEdges(from)) {
            if (e.getTo().equals(to))
                return e;
        }
        return null;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------
    public List<Flight> getFlights() {
        return flights;
    }

    /** O(1) average-case flight lookup by ID, backed by the HashMap registry. */
    public Flight findFlightById(String id) {
        return flightRegistry.get(id);
    }

    public List<Flight> getActiveOrScheduledFlights() {
        List<Flight> result = new ArrayList<>();
        for (Flight f : flights) {
            if (!Flight.STATUS_LANDED.equals(f.getStatus())) {
                result.add(f);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------
    // Simulation tick — called every frame from the JavaFX AnimationTimer
    // -----------------------------------------------------------------
    // Flights in an active emergency move this many times faster, so the
    // diversion/landing visibly happens quickly on screen.
    private static final double EMERGENCY_SPEED_MULTIPLIER = 3.0;

    public void tick(double deltaSeconds) {
        long now = System.currentTimeMillis();
        for (Flight f : flights) {
            if (Flight.STATUS_LANDED.equals(f.getStatus()))
                continue;

            if (Flight.STATUS_SCHEDULED.equals(f.getStatus())) {
                if (now >= f.getDepartureTime()) {
                    f.setStatus(Flight.STATUS_ACTIVE);
                    log("Flight " + f.getFlightId() + " departed from " + f.getDeparture().getName());
                }
                continue;
            }

            double effectiveDelta = Flight.STATUS_EMERGENCY.equals(f.getStatus())
                    ? deltaSeconds * EMERGENCY_SPEED_MULTIPLIER
                    : deltaSeconds;
            advanceFlight(f, effectiveDelta);
        }
    }

    private void advanceFlight(Flight f, double deltaSeconds) {
        Airport next = f.getNextAirport();
        if (next == null) {
            // Already at the last waypoint in its route -> land it.
            landFlight(f);
            return;
        }

        Airport current = f.getCurrentAirport();
        Edge edge = edgeBetween(current, next);
        double legMinutes = (edge != null) ? edge.getEstimatedFlightTime() : 30.0;
        double legSeconds = Math.max(legMinutes * 6, 8.0); // compressed simulation time-scale

        double progress = f.getLegProgress() + (deltaSeconds / legSeconds);
        if (progress >= 1.0) {
            f.setLegProgress(0.0);
            f.setCurrentRouteIndex(f.getCurrentRouteIndex() + 1);
            f.setCurrentX(next.getX());
            f.setCurrentY(next.getY());
            if (f.isLastLeg()) {
                landFlight(f);
            }
        } else {
            f.setLegProgress(progress);
            double[] pos = laneOffsetPosition(current, next, progress);
            f.setCurrentX(pos[0]);
            f.setCurrentY(pos[1]);
        }
    }

    /**
     * Computes the flight's on-screen position for the given leg progress,
     * offset laterally to the right of its direction of travel by a fixed
     * lane distance. This is the key fix for head-on traffic: a flight going
     * A -> B and another going B -> A on the very same route are each offset
     * to their own right-hand side of the centerline, so they fly on two
     * parallel tracks and never actually occupy the same point in space,
     * exactly like real-world opposite-direction air lanes.
     */
    private double[] laneOffsetPosition(Airport current, Airport next, double progress) {
        double baseX = current.getX() + (next.getX() - current.getX()) * progress;
        double baseY = current.getY() + (next.getY() - current.getY()) * progress;

        double dx = next.getX() - current.getX();
        double dy = next.getY() - current.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-6) {
            return new double[] { baseX, baseY };
        }

        // Unit vector rotated -90 degrees = the "right-hand" side of travel direction.
        double perpX = dy / length;
        double perpY = -dx / length;

        // Taper the offset to zero at both ends of the leg (departure/arrival), so the
        // plane smoothly eases into and out of its lane instead of visually snapping.
        double taper = Math.sin(Math.PI * progress);
        double offset = LANE_OFFSET_PIXELS * taper;

        return new double[] { baseX + perpX * offset, baseY + perpY * offset };
    }

    private void landFlight(Flight f) {
        f.setStatus(Flight.STATUS_LANDED);
        Airport landingAirport = f.getRoutePath().get(f.getRoutePath().size() - 1);
        landingAirport.requestLanding(f);
        Flight served = landingAirport.pollRunway();
        log("Flight " + f.getFlightId() + " landed at " + landingAirport.getName()
                + (served == f ? " (runway cleared)" : ""));
    }

    // -----------------------------------------------------------------
    // Collision Avoidance — background thread, runs every second
    // -----------------------------------------------------------------
    private void startCollisionMonitor() {
        collisionExecutor.scheduleAtFixedRate(this::checkCollisions, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Runs on the background CollisionAvoidanceThread. Only *reads* flight
     * positions here (snapshot iteration over the thread-safe list), then
     * hands any required mutation off to the JavaFX Application Thread via
     * Platform.runLater so that all writes to shared Flight/Airport state
     * happen on a single thread, avoiding races with the animation loop.
     */
    private void checkCollisions() {
        List<Flight> snapshot = new ArrayList<>();
        for (Flight f : flights) {
            if (Flight.STATUS_ACTIVE.equals(f.getStatus()) || Flight.STATUS_EMERGENCY.equals(f.getStatus())) {
                snapshot.add(f);
            }
        }

        for (int i = 0; i < snapshot.size(); i++) {
            for (int j = i + 1; j < snapshot.size(); j++) {
                Flight a = snapshot.get(i);
                Flight b = snapshot.get(j);
                double dx = a.getCurrentX() - b.getCurrentX();
                double dy = a.getCurrentY() - b.getCurrentY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < SAFE_LIMIT_PIXELS) {
                    Platform.runLater(() -> resolveConflict(a, b));
                }
            }
        }
    }

    /**
     * Executed on the JavaFX Application Thread: separate two conflicting flights
     * by altitude and re-routing.
     */
    private void resolveConflict(Flight a, Flight b) {
        long now = System.currentTimeMillis();
        a.setLastConflictAtMillis(now);
        b.setLastConflictAtMillis(now);

        // Quick separation: bump one flight's altitude so they are no longer co-planar
        if (a.getAltitude() == b.getAltitude()) {
            b.setAltitude(b.getAltitude() + 2000);
            log("Collision risk: " + a.getFlightId() + " & " + b.getFlightId()
                    + " separated by altitude (" + b.getFlightId() + " -> " + b.getAltitude() + " ft)");
        }

        // Attempt to dynamically recalculate flight B's remaining path away from A's
        // current leg
        rerouteAroundConflict(b);
    }

    private void rerouteAroundConflict(Flight f) {
        if (f.getCurrentAirport().equals(f.getDestination()))
            return;

        List<Airport> newRemaining = buildDivertedRoute(f, f.getDestination());
        if (newRemaining != null) {
            f.divertRemainingRoute(newRemaining);
            log("Flight " + f.getFlightId() + " dynamically re-routed to avoid collision.");
        }
    }

    /**
     * Builds a new remaining-route list for a flight that is diverting to
     * newDestination, WITHOUT disturbing the leg it is already committed to
     * mid-air. The in-progress leg (currentAirport -> nextAirport) is kept
     * exactly as-is; Dijkstra is only used to plan the path *after* that
     * committed leg, so the plane changes course in real time from its
     * current position instead of snapping back to its departure point.
     * Returns null if no viable path could be found at all.
     */
    private List<Airport> buildDivertedRoute(Flight f, Airport newDestination) {
        Airport from = f.getCurrentAirport(); // origin of the leg currently in progress
        Airport next = f.getNextAirport(); // target of the leg currently in progress (already committed)

        if (next == null) {
            // Flight has already arrived at its last waypoint; nothing left to divert
            // mid-air.
            List<Airport> direct = graph.dijkstra(from, newDestination);
            return direct.isEmpty() ? null : direct;
        }

        if (next.equals(newDestination)) {
            // The committed leg already ends exactly at the new destination.
            List<Airport> route = new ArrayList<>();
            route.add(from);
            route.add(next);
            return route;
        }

        List<Airport> tail = graph.dijkstra(next, newDestination);
        if (tail.isEmpty()) {
            // No path onward from the committed next-airport; fall back to a
            // fresh path from the current leg's origin (small course change only).
            List<Airport> fallback = graph.dijkstra(from, newDestination);
            return fallback.isEmpty() ? null : fallback;
        }

        List<Airport> combined = new ArrayList<>();
        combined.add(from);
        combined.addAll(tail); // tail already starts with 'next'
        return combined;
    }

    // -----------------------------------------------------------------
    // Emergency handling
    // -----------------------------------------------------------------
    /**
     * Applies a diversion to airport 'safe'. If 'safe' is the airport the
     * plane just departed from (the origin of its currently in-progress leg),
     * the plane visibly turns around and flies backward to it. Otherwise it
     * continues its already-committed leg forward and then paths onward to
     * 'safe' via Dijkstra.
     */
    private void applyDiversion(Flight f, Airport safe) {
        Airport from = f.getCurrentAirport();
        Airport next = f.getNextAirport();

        if (next != null && from.equals(safe)) {
            // Nearest safe airport is behind the plane -> U-turn back to it.
            List<Airport> reversedLeg = new ArrayList<>();
            reversedLeg.add(next);
            reversedLeg.add(from);
            f.reverseCurrentLeg(reversedLeg);
            log("Flight " + f.getFlightId() + " is nearer to its departure airport "
                    + from.getName() + " -- turning back.");
            return;
        }

        List<Airport> forwardRoute = buildDivertedRoute(f, safe);
        if (forwardRoute != null) {
            f.divertRemainingRoute(forwardRoute);
        }
    }

    public boolean createEmergency(String flightId, String emergencyType) {
        Flight f = findFlightById(flightId);
        if (f == null || Flight.STATUS_LANDED.equals(f.getStatus()))
            return false;

        if (Flight.STATUS_EMERGENCY.equals(f.getStatus())) {
            // Diversion already locked in for this flight -- do not pick a
            // different nearest airport on repeated clicks.
            log("Flight " + f.getFlightId() + " is already diverting to "
                    + f.getDestination().getName() + "; ignoring duplicate emergency request.");
            return false;
        }

        f.setStatus(Flight.STATUS_EMERGENCY);
        f.setEmergencyType(emergencyType);
        log("EMERGENCY (" + emergencyType + ") declared on flight " + f.getFlightId());

        Set<Airport> excluded = new HashSet<>();
        if ("Weather".equals(emergencyType)) {
            f.setHasWeatherEmergency(true);
            // The weather is assumed to be affecting the remainder of the original
            // route/destination, so we must divert away from it entirely.
            excluded.add(f.getDestination());
        }
        // Fuel Shortage: no exclusions -- the genuinely nearest airport from the
        // plane's current live position wins, even if that happens to be behind it.

        // Measure distance from the plane's actual current position (not a graph
        // node), so the diversion target reflects where it physically is right now.
        Airport safe = graph.findNearestAirportByPosition(f.getCurrentX(), f.getCurrentY(), excluded);
        if (safe == null) {
            safe = f.getDestination();
        }

        applyDiversion(f, safe);
        f.setDestination(safe);

        // Override normal runway traffic: register an emergency landing request
        // immediately.
        safe.requestLanding(f);
        log("Flight " + f.getFlightId() + " diverting to " + safe.getName()
                + " (priority emergency landing).");

        if (onEmergencyDeclared != null) {
            onEmergencyDeclared.accept(f);
        }
        return true;
    }

    public void shutdown() {
        collisionExecutor.shutdownNow();
    }
}

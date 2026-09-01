package com.atc.graph;

import com.atc.model.Airport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class FlightGraph {

    private final Map<Airport, List<Edge>> adjacency = new HashMap<>();
    private final List<Airport> airports = new ArrayList<>();

    public void addAirport(Airport airport) {
        adjacency.putIfAbsent(airport, new ArrayList<>());
        airports.add(airport);
    }

    public List<Airport> getAirports() {
        return airports;
    }

    public void addRoute(Airport a, Airport b, double distance, double estimatedFlightTime) {
        if (a == null || b == null) {
            return;
        }

        adjacency.computeIfAbsent(a, key -> new ArrayList<>()).add(new Edge(b, distance, estimatedFlightTime));
        adjacency.computeIfAbsent(b, key -> new ArrayList<>()).add(new Edge(a, distance, estimatedFlightTime));

        if (!airports.contains(a)) {
            airports.add(a);
        }
        if (!airports.contains(b)) {
            airports.add(b);
        }
    }

    public List<Edge> getEdges(Airport airport) {
        return adjacency.getOrDefault(airport, Collections.emptyList());
    }

    public List<Airport> dijkstra(Airport start, Airport end) {
        Map<Airport, Double> dist = new HashMap<>();
        Map<Airport, Airport> prev = new HashMap<>();
        Set<Airport> visited = new HashSet<>();

        for (Airport a : airports) {
            dist.put(a, Double.POSITIVE_INFINITY);
        }
        dist.put(start, 0.0);

        PriorityQueue<Airport> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.offer(start);

        while (!pq.isEmpty()) {
            Airport current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(end)) break;

            for (Edge edge : getEdges(current)) {
                Airport neighbor = edge.getTo();
                if (visited.contains(neighbor)) continue;
                double newDist = dist.get(current) + edge.getDistance();
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, current);
                    pq.offer(neighbor);
                }
            }
        }

        if (dist.get(end) == null || Double.isInfinite(dist.get(end))) {
            return new ArrayList<>(); 
        }

  
        Deque<Airport> path = new ArrayDeque<>();
        Airport step = end;
        while (step != null) {
            path.addFirst(step);
            if (step.equals(start)) break;
            step = prev.get(step);
        }
        return new ArrayList<>(path);
    }


    public Airport findNearestAirportByPosition(double x, double y, Set<Airport> excluded) {
        Airport best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Airport a : airports) {
            if (excluded.contains(a)) continue;
            double dx = a.getX() - x;
            double dy = a.getY() - y;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

 
    public Airport findNearestAirport(Airport from, Set<Airport> excluded) {
        Map<Airport, Double> dist = new HashMap<>();
        Set<Airport> visited = new HashSet<>();
        for (Airport a : airports) {
            dist.put(a, Double.POSITIVE_INFINITY);
        }
        dist.put(from, 0.0);

        PriorityQueue<Airport> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.offer(from);

        Airport best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        while (!pq.isEmpty()) {
            Airport current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (!current.equals(from) && !excluded.contains(current) && dist.get(current) < bestDist) {
                best = current;
                bestDist = dist.get(current);
            }

            for (Edge edge : getEdges(current)) {
                Airport neighbor = edge.getTo();
                if (visited.contains(neighbor)) continue;
                double newDist = dist.get(current) + edge.getDistance();
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    pq.offer(neighbor);
                }
            }
        }
        return best;
    }
}

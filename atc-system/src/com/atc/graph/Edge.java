package com.atc.graph;

import com.atc.model.Airport;

public class Edge {

    private final Airport to;
    private final double distance;           
    private final double estimatedFlightTime; 
    public Edge(Airport to, double distance, double estimatedFlightTime) {
        this.to = to;
        this.distance = distance;
        this.estimatedFlightTime = estimatedFlightTime;
    }

    public Airport getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    public double getEstimatedFlightTime() {
        return estimatedFlightTime;
    }
}

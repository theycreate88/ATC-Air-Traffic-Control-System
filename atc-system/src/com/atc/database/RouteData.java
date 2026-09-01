package com.atc.database;

public class RouteData {

    private final String source;
    private final String destination;
    private final double distance;
    private final double flightTime;

    public RouteData(String source,
                     String destination,
                     double distance,
                     double flightTime) {

        this.source = source;
        this.destination = destination;
        this.distance = distance;
        this.flightTime = flightTime;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    public double getFlightTime() {
        return flightTime;
    }
}
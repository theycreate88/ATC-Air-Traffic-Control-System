package com.atc.graph;

import com.atc.model.Airport;

public class FlightGraphTest {
    public static void main(String[] args) {
        FlightGraph graph = new FlightGraph();
        Airport a = new Airport("A", 0, 0);
        graph.addAirport(a);

        Airport missing = new Airport("B", 10, 10);
        graph.addRoute(a, missing, 10.0, 15.0);

        System.out.println("route-addition-completed");
    }
}

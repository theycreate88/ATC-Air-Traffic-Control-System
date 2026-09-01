package com.atc.database;

public class TestRouteDAO {

    public static void main(String[] args) {

        RouteDAO dao = new RouteDAO();

        for (RouteData r : dao.getAllRoutes()) {
            System.out.println(
                r.getSource() + " -> " +
                r.getDestination() +
                " | Distance: " + r.getDistance() +
                " | Time: " + r.getFlightTime()
            );
        }
    }
}
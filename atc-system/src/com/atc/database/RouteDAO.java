package com.atc.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RouteDAO {

    public List<RouteData> getAllRoutes() {

        List<RouteData> routes = new ArrayList<>();

        String sql = """
                SELECT
                    a1.AirportName AS SourceAirport,
                    a2.AirportName AS DestinationAirport,
                    r.Distance,
                    r.FlightTime
                FROM Route r
                JOIN Airport a1
                    ON r.SourceAirport = a1.AirportID
                JOIN Airport a2
                    ON r.DestinationAirport = a2.AirportID
                """;

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                routes.add(
                        new RouteData(
                                rs.getString("SourceAirport"),
                                rs.getString("DestinationAirport"),
                                rs.getDouble("Distance"),
                                rs.getDouble("FlightTime")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return routes;
    }
}
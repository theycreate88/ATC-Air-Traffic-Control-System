package com.atc.database;

import com.atc.model.Airport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AirportDAO {

    public List<Airport> getAllAirports() {

        List<Airport> airports = new ArrayList<>();

        String sql = """
                SELECT AirportName, XCoordinate, YCoordinate
                FROM Airport
                ORDER BY AirportName
                """;

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("AirportName");
                int x = rs.getInt("XCoordinate");
                int y = rs.getInt("YCoordinate");

                Airport airport = new Airport(name, x, y);

                airports.add(airport);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return airports;
    }
}
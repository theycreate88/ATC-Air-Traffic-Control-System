package com.atc.database;

import com.atc.model.Airport;

import java.util.List;

public class TestAirportDAO {

    public static void main(String[] args) {

        AirportDAO dao = new AirportDAO();

        List<Airport> airports = dao.getAllAirports();

        for (Airport airport : airports) {

            System.out.println(
                    airport.getName() +
                    " (" +
                    airport.getX() +
                    "," +
                    airport.getY() +
                    ")"
            );
        }
    }
}
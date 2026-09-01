package com.atc.database;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try (Connection con = DBConnection.getConnection()) {

            System.out.println("================================");
            System.out.println("Connected to SQL Server!");
            System.out.println("Database: " + con.getCatalog());
            System.out.println("================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
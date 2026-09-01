package com.atc.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:sqlserver://localhost:1433;"
            + "databaseName=AirTrafficDB;"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    // Replace with YOUR sa password
    private static final String USER = "sa";
    private static final String PASSWORD = "Baran08";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
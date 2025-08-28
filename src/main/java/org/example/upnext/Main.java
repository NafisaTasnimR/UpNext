package org.example.upnext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    // Database connection parameters
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "1521";
    private static final String DB_SERVICE_NAME = "XE";
    private static final String DB_USERNAME = "C##UPNEXT";
    private static final String DB_PASSWORD = "myPass123";

    // Use service name format (recommended for 12c/19c/21c)
    private static final String CONNECTION_URL =
            "jdbc:oracle:thin:@//" + DB_HOST + ":" + DB_PORT + "/" + DB_SERVICE_NAME;

    public static void main(String[] args) {
        System.out.println("=== Oracle Database Connection Test ===");
        Connection conn = null;

        try {
            // Load Oracle JDBC driver (needed only for old ojdbc drivers)
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Establish connection
            conn = DriverManager.getConnection(CONNECTION_URL, DB_USERNAME, DB_PASSWORD);
            System.out.println(" Connected to Oracle Database successfully!");

        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println(" Connection failed!");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println(" Connection closed.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
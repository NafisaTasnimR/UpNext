package org.example.upnext;

import java.sql.*;

public class Main {

    // Database connection parameters
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "1522";
    private static final String DB_SERVICE_NAME = "XEPDB1";
    private static final String DB_USERNAME = "UPNEXT";
    private static final String DB_PASSWORD = "myPass";

    // Use service name format (recommended for 12c/19c/21c)
    private static final String CONNECTION_URL =
            "jdbc:oracle:thin:@//" + DB_HOST + ":" + DB_PORT + "/" + DB_SERVICE_NAME;

    public static void main(String[] args) {
        System.out.println("=== Oracle Database Connection Test ===");

        // Try both driver class names (harmless if one fails)
        try {
            try { Class.forName("oracle.jdbc.driver.OracleDriver"); }
            catch (ClassNotFoundException ignore) { Class.forName("oracle.jdbc.OracleDriver"); }
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found!");
            e.printStackTrace();
            return;
        }

        try (Connection conn = DriverManager.getConnection(CONNECTION_URL, DB_USERNAME, DB_PASSWORD)) {
            System.out.println(" Connected!");
            System.out.println(" URL: " + CONNECTION_URL);
            System.out.println(" USERNAME: " + DB_USERNAME);
            System.out.println();

            // --- Show where we actually connected (super helpful for PDB/schema mismatches) ---
            String envSql =
                    "select " +
                            " sys_context('USERENV','SESSION_USER') as session_user," +
                            " sys_context('USERENV','CURRENT_SCHEMA') as current_schema," +
                            " sys_context('USERENV','SERVICE_NAME') as service_name," +
                            " sys_context('USERENV','CON_NAME') as con_name " +
                            "from dual";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(envSql)) {
                if (rs.next()) {
                    System.out.println(" Session User : " + rs.getString("session_user"));
                    System.out.println(" CurrentSchema: " + rs.getString("current_schema"));
                    System.out.println(" Service/PDB  : " + rs.getString("service_name"));
                    System.out.println(" Container    : " + rs.getString("con_name"));
                    System.out.println();
                }
            }

            // --- Count rows first ---
            long count = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM flower")) {
                if (rs.next()) count = rs.getLong(1);
            } catch (SQLException e) {
                // If table not found in current schema, say so
                if (e.getErrorCode() == 942) {
                    System.err.println("Table FLOWER not found in current schema. " +
                            "Either connect as the owner or fully-qualify (e.g., EX.FLOWER) " +
                            "and ensure SELECT privilege is granted.");
                    throw e;
                } else {
                    throw e;
                }
            }

            System.out.println(" Row count in FLOWER: " + count);
            System.out.println();

            // --- Print rows ---
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM flower")) {
                System.out.println("Rows in FLOWER:");
                int printed = 0;
                while (rs.next()) {
                    System.out.println(" - " + rs.getString(1));
                    printed++;
                }
                if (printed == 0) System.out.println(" (no rows)");
            }

            // --- (Optional) Show where a FLOWER table is visible to this user ---
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT owner FROM all_tables WHERE table_name='FLOWER' ORDER BY owner");
                 ResultSet rs = ps.executeQuery()) {
                System.out.print("\nFLOWER visible via ALL_TABLES under owners: ");
                boolean any = false;
                while (rs.next()) {
                    System.out.print(rs.getString(1) + " ");
                    any = true;
                }
                if (!any) System.out.print("(none - maybe you’re in a different user/PDB or lack privileges)");
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println(" Connection or query failed!");
            System.err.println(" SQLState: " + e.getSQLState());
            System.err.println(" ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
    }


}
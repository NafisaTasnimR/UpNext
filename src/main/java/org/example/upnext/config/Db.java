package org.example.upnext.config;


import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central JDBC connection provider.
 * Reads config.properties from resources.
 */
public class Db {
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            if (in != null) props.load(in);
            url = props.getProperty("jdbc.url");
            user = props.getProperty("jdbc.user");
            password = props.getProperty("jdbc.password");

            // Load Oracle JDBC driver explicitly (optional for newer drivers)
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}


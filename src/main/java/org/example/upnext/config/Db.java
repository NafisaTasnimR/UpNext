package org.example.upnext.config; // adjust root if needed

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Db {
    private static String url, user, password;

    static {
        try {
            Properties p = loadProps();
            url = firstNonNull(p.getProperty("jdbc.url"), p.getProperty("db.url"));
            user = firstNonNull(p.getProperty("jdbc.user"), p.getProperty("db.user"));
            password = firstNonNull(p.getProperty("jdbc.password"), p.getProperty("db.password"));

            System.out.println("[Db] props loaded = " + p.stringPropertyNames());
            System.out.println("[Db] jdbc.url     = " + url);
            System.out.println("[Db] jdbc.user    = " + user);

            if (url == null) throw new IllegalStateException("jdbc.url is null (config not found or wrong keys)");
            Class.forName("oracle.jdbc.driver.OracleDriver"); // ok for ojdbc11/17
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DB config", e);
        }
    }

    private static Properties loadProps() throws Exception {
        List<String> candidates = List.of(
                "config.properties",
                "application.properties",
                "config/config.properties",
                "config/db.properties"
        );
        Properties p = new Properties();
        for (String name : candidates) {
            try (InputStream in = Db.class.getClassLoader().getResourceAsStream(name)) {
                if (in != null) {
                    p.load(in);
                    System.out.println("[Db] loaded: " + name);
                    return p;
                }
            }
        }
        // extra diagnostics: list a few known resources
        System.out.println("[Db] Could not find any of " + candidates + " on classpath.");
        return p; // empty -> will trip the null check
    }

    private static String firstNonNull(String a, String b) { return a != null ? a : b; }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}

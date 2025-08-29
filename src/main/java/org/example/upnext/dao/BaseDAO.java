package org.example.upnext.dao;

import org.example.upnext.config.Db;

import java.sql.*;

public abstract class BaseDAO {
    protected Connection getConn() throws SQLException {
        return Db.getConnection();
    }
    protected void close(AutoCloseable c) { if (c != null) try { c.close(); } catch (Exception ignored) {} }

    protected long nextVal(Connection conn, String seqName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + seqName + ".NEXTVAL FROM DUAL");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            throw new SQLException("No NEXTVAL for seq: " + seqName);
        }
    }
}

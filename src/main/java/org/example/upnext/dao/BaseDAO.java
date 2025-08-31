package org.example.upnext.dao;

import org.example.upnext.config.Db;
import org.example.upnext.auth.AuthContext;

import java.sql.*;

public abstract class BaseDAO {
//    protected Connection getConn() throws SQLException {
//        return Db.getConnection();
//    }
    protected Connection getConn() throws SQLException {
        Connection c = Db.getConnection();  // your existing line
        String id = AuthContext.getUsername();
        if (id != null && !id.isBlank()) {
            try (CallableStatement cs = c.prepareCall("{ call DBMS_SESSION.SET_IDENTIFIER(?) }")) {
                cs.setString(1, id);   // app username goes to CLIENT_IDENTIFIER
                cs.execute();
            }
        }
        return c;
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

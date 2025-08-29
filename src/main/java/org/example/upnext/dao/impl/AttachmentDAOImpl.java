package org.example.upnext.dao.impl;

import org.example.upnext.dao.AttachmentDAO;
import org.example.upnext.dao.BaseDAO;
import org.example.upnext.model.Attachment;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class AttachmentDAOImpl extends BaseDAO implements AttachmentDAO {

    private Attachment map(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setAttachmentId(rs.getLong("ATTACHMENT_ID"));
        a.setTaskId(rs.getLong("TASK_ID"));
        a.setFileName(rs.getString("FILE_NAME"));
        a.setFilePath(rs.getString("FILE_PATH"));
        a.setUploadedBy(rs.getLong("UPLOADED_BY"));
        Timestamp t = rs.getTimestamp("UPLOADED_AT");
        if (t != null) a.setUploadedAt(t.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        return a;
    }

    @Override public long create(Attachment a) throws SQLException {
        String sql = "INSERT INTO ATTACHMENTS (ATTACHMENT_ID, TASK_ID, FILE_NAME, FILE_PATH, UPLOADED_BY) VALUES (?,?,?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            long id = nextVal(c, "ATTACHMENTS_SEQ");
            ps.setLong(1, id);
            ps.setLong(2, a.getTaskId());
            ps.setString(3, a.getFileName());
            ps.setString(4, a.getFilePath());
            ps.setLong(5, a.getUploadedBy());
            ps.executeUpdate();
            return id;
        }
    }

    @Override public List<Attachment> findByTask(long taskId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM ATTACHMENTS WHERE TASK_ID=? ORDER BY UPLOADED_AT DESC")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attachment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public void delete(long attachmentId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM ATTACHMENTS WHERE ATTACHMENT_ID=?")) {
            ps.setLong(1, attachmentId); ps.executeUpdate();
        }
    }
}


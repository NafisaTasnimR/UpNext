package org.example.upnext.dao.impl;


import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.CommentDAO;
import org.example.upnext.model.Comment;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class CommentDAOImpl extends BaseDAO implements CommentDAO {

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setCommentId(rs.getLong("COMMENT_ID"));
        c.setTaskId(rs.getLong("TASK_ID"));
        c.setUserId(rs.getLong("USER_ID"));
        c.setContent(rs.getString("CONTENT"));
        Timestamp t = rs.getTimestamp("CREATED_AT");
        if (t != null) c.setCreatedAt(t.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        return c;
    }

    @Override public long create(Comment cmt) throws SQLException {
        String sql = "INSERT INTO COMMENTS (COMMENT_ID, TASK_ID, USER_ID, CONTENT) VALUES (COMMENTS_SEQ.NEXTVAL, ?, ?, ?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql, new String[]{"COMMENT_ID"})) {
            ps.setLong(1, cmt.getTaskId());
            ps.setLong(2, cmt.getUserId());
            ps.setString(3, cmt.getContent());
            ps.executeUpdate();
            // Return generated ID (or query seq.currval)
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            // fallback
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COMMENTS_SEQ.CURRVAL FROM DUAL")) {
                rs.next(); return rs.getLong(1);
            }
        }
    }

    @Override public List<Comment> findByTask(long taskId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM COMMENTS WHERE TASK_ID=? ORDER BY CREATED_AT DESC")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Comment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public void delete(long commentId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM COMMENTS WHERE COMMENT_ID=?")) {
            ps.setLong(1, commentId); ps.executeUpdate();
        }
    }
}


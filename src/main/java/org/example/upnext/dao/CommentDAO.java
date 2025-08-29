package org.example.upnext.dao;


import org.example.upnext.model.Comment;

import java.sql.SQLException;
import java.util.List;

public interface CommentDAO {
    long create(Comment c) throws SQLException;
    List<Comment> findByTask(long taskId) throws SQLException;
    void delete(long commentId) throws SQLException;
}


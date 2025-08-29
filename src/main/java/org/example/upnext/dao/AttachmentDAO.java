package org.example.upnext.dao;


import org.example.upnext.model.Attachment;

import java.sql.SQLException;
import java.util.List;

public interface AttachmentDAO {
    long create(Attachment a) throws SQLException;
    List<Attachment> findByTask(long taskId) throws SQLException;
    void delete(long attachmentId) throws SQLException;
}


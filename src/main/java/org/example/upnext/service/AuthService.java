package org.example.upnext.service;


import org.example.upnext.dao.UserDAO;
import org.example.upnext.model.User;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Optional;


public class AuthService {
    private final UserDAO userDAO;
    public AuthService(UserDAO userDAO) { this.userDAO = userDAO; }


    public Optional<User> login(String username, String passwordPlain) throws SQLException {
        Optional<User> u = userDAO.findByUsername(username);
        if (u.isPresent() && "ACTIVE".equals(u.get().getStatus())) {
            String hash = sha256(passwordPlain);
            if (hash.equals(u.get().getPasswordHash())) return u;
        }
        return Optional.empty();
    }


    public long register(User user, String passwordPlain) throws SQLException {
        user.setPasswordHash(sha256(passwordPlain));
        if (user.getStatus() == null) user.setStatus("ACTIVE");
        return userDAO.create(user);
    }


    public void changePassword(long userId, String newPasswordPlain) throws SQLException {
        User u = userDAO.findById(userId).orElseThrow(() -> new SQLException("User not found"));
        u.setPasswordHash(sha256(newPasswordPlain));
        userDAO.update(u);
    }


    private static String sha256(String v) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(v.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
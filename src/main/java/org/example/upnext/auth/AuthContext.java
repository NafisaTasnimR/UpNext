package org.example.upnext.auth;

public final class AuthContext {
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private AuthContext() {}
    public static void setUsername(String username) { USERNAME.set(username); }
    public static String getUsername() { return USERNAME.get(); }
    public static void clear() { USERNAME.remove(); }
}

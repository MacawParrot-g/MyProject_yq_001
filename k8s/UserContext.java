package org.example.common;

public class UserContext {

    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void setIp(String ip) {
        IP.set(ip);
    }

    public static String getIp() {
        return IP.get();
    }

    public static void clear() {
        USERNAME.remove();
        IP.remove();
    }
}
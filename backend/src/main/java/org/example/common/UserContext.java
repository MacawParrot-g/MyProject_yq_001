package org.example.common;

public class UserContext {

    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> IP = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_TYPE = new ThreadLocal<>();

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

    public static void setUserType(String userType) {
        USER_TYPE.set(userType);
    }

    public static String getUserType() {
        return USER_TYPE.get();
    }

    public static void clear() {
        USERNAME.remove();
        IP.remove();
        USER_TYPE.remove();
    }
}
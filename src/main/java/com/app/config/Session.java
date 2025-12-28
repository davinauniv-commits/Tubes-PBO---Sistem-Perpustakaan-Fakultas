package com.app.config;

public class Session {

    private static int adminId;
    private static String adminUsername;

    public static int getAdminId() {
        return adminId;
    }

    public static void setAdminId(int id) {
        adminId = id;
    }

    public static String getAdminUsername() {
        return adminUsername;
    }

    public static void setAdminUsername(String username) {
        adminUsername = username;
    }

    public static void clear() {
        adminId = 0;
        adminUsername = null;
    }
}
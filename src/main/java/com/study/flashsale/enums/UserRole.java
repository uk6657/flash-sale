package com.study.flashsale.enums;

public enum UserRole {

    USER,
    ADMIN;

    public static boolean isAdmin(String role) {
        return ADMIN.name().equals(role);
    }
}

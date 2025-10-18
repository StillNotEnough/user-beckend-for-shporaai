package com.amazingshop.personal.userservice.enums;

public enum Subject {
    MATH,
    PROGRAMMING,
    ENGLISH,
    GENERAL;

    // Метод для получения enum из строки (case-insensitive)
    public static Subject fromString(String value) {
        if (value == null) {
            return GENERAL;
        }

        try {
            return Subject.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL; // Значение по умолчанию
        }
    }

    // Метод для получения строки в нижнем регистре (для frontend)
    public String toLowerCase() {
        return this.name().toLowerCase();
    }
}
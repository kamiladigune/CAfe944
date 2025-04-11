package com.cafe94.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ValidationUtils {
    
    public static <T> T requireNonNull(T obj, String fieldName) {
        return Objects.requireNonNull(obj, fieldName + " cannot be null.");
    }

    public static void requireNonEmpty(List<?> list, String fieldName) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(fieldName +
            " cannot be empty.");
        }
    }
    
    public static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " cannot be null.");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName +
            " cannot be blank.");
        }
        return value;
    }

public static double requireNonNegative(double value, String fieldName) {
    if (value < 0) {
        throw new IllegalArgumentException(fieldName +
        " cannot be negative. Value: " + value);
    }
    return value;
} 

public static int requireNonNegative(int value, String fieldName) {
     if (value < 0) {
         throw new IllegalArgumentException(fieldName +
         " cannot be negative. Value: " + value);
     }
     return value;
}

// Needed by Staff, StaffDetailsDto
 public static <T> List<T> nullSafeList(List<T> list) {
    return list == null ? Collections.emptyList() : list;
}

// Needed by OrderService, ReportingService, etc.
public static int requirePositive(int value, String fieldName) {
     if (value <= 0) {
         throw new IllegalArgumentException(fieldName + " must be positive. Value: " + value);
     }
     return value;
}

}
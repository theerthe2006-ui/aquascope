package com.aquascope.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp,
    Map<String, String> errors
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, Instant.now(), errors);
    }
}

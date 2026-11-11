package com.aquascope.dto;

import java.time.Instant;

public record HealthResponse(
    String status,
    String application,
    String version,
    String environment,
    Instant timestamp,
    DatabaseHealth database
) {
    public record DatabaseHealth(
        boolean connected,
        String databaseType,
        String details
    ) {}
}

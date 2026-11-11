package com.aquascope.dto;

import com.aquascope.entity.DataOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record SensorReadingDto(
    Long id,
    @NotNull(message = "Device ID is required")
    Long deviceId,
    String deviceIdentifier,
    @NotNull(message = "Reading timestamp is required")
    Instant readingTimestamp,
    @NotBlank(message = "Metric type is required")
    String metricType,
    @NotNull(message = "Metric value is required")
    BigDecimal metricValue,
    @NotBlank(message = "Unit is required")
    String unit,
    DataOrigin dataOrigin,
    String rawPayload,
    Instant createdAt
) {}

package com.aquascope.dto;

import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.SampleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record WaterSampleDto(
    Long id,
    @NotBlank(message = "Sample identifier is required")
    String sampleIdentifier,
    @NotNull(message = "Monitoring site ID is required")
    Long monitoringSiteId,
    String monitoringSiteCode,
    String monitoringSiteName,
    String collectedByUsername,
    @NotNull(message = "Collection date is required")
    Instant collectionDate,
    @NotNull(message = "Sample volume is required")
    @DecimalMin(value = "0.0001", message = "Sample volume must be greater than zero")
    BigDecimal sampleVolume,
    String volumeUnit,
    BigDecimal samplingDepthMeters,
    BigDecimal waterTemperatureCelsius,
    BigDecimal phLevel,
    BigDecimal turbidityNtu,
    @NotBlank(message = "Collection source is required")
    String collectionSource,
    String weatherConditions,
    DataOrigin dataOrigin,
    SampleStatus status,
    String notes,
    Instant createdAt
) {}

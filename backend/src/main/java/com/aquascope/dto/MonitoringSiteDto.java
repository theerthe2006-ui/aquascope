package com.aquascope.dto;

import com.aquascope.entity.WaterSourceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record MonitoringSiteDto(
    Long id,
    @NotBlank(message = "Site code is required")
    String code,
    @NotBlank(message = "Site name is required")
    String name,
    String description,
    @NotNull(message = "Water source category is required")
    WaterSourceCategory waterSourceCategory,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal elevationMeters,
    String createdByUsername,
    boolean active,
    Instant createdAt
) {}

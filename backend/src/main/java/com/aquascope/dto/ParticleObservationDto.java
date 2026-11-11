package com.aquascope.dto;

import com.aquascope.entity.IdentificationMethod;
import com.aquascope.entity.ParticleShape;
import com.aquascope.entity.PolymerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ParticleObservationDto(
    Long id,
    @Min(value = 1, message = "Particle count must be at least 1")
    int particleCount,
    @NotNull(message = "Size in micrometers is required")
    @DecimalMin(value = "0.01", message = "Size must be positive")
    BigDecimal sizeMicrometers,
    BigDecimal lengthMicrometers,
    BigDecimal widthMicrometers,
    @NotNull(message = "Particle shape is required")
    ParticleShape shape,
    String color,
    @NotNull(message = "Polymer type is required")
    PolymerType polymerType,
    @NotNull(message = "Identification method is required")
    IdentificationMethod identificationMethod,
    String confidenceLevel,
    BigDecimal spectralMatchScore,
    String notes
) {}

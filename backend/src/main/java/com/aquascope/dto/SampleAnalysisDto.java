package com.aquascope.dto;

import com.aquascope.entity.AnalysisStatus;
import com.aquascope.entity.DataOrigin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SampleAnalysisDto(
    Long id,
    @NotNull(message = "Water sample ID is required")
    Long waterSampleId,
    String sampleIdentifier,
    String analystUsername,
    @NotNull(message = "Analysis date is required")
    Instant analysisDate,
    @NotBlank(message = "Analysis method is required")
    String analysisMethod,
    String equipmentModel,
    String magnification,
    String preparationMethod,
    String filterMaterial,
    BigDecimal filterPoreSizeUm,
    int totalParticlesCounted,
    BigDecimal concentrationParticlesPerLiter,
    AnalysisStatus status,
    DataOrigin dataOrigin,
    String notes,
    @Valid
    List<ParticleObservationDto> observations,
    Instant createdAt
) {}

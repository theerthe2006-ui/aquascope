package com.aquascope.dto;

import java.time.Instant;

public record SampleImageDto(
    Long id,
    Long waterSampleId,
    Long sampleAnalysisId,
    Long uploadedByUserId,
    String uploadedByUsername,
    String fileName,
    String storedFilePath,
    String contentType,
    Long fileSizeBytes,
    String imagingModality,
    String magnification,
    String description,
    Instant createdAt
) {}

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
    String objectKey,
    String sha256Checksum,
    String storageStatus,
    String storageBackend,
    String contentType,
    Long fileSizeBytes,
    String imagingModality,
    String magnification,
    String description,
    Instant createdAt
) {}

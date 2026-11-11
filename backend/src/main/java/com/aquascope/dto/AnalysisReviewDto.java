package com.aquascope.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record AnalysisReviewDto(
    Long id,
    Long sampleAnalysisId,
    String reviewerUsername,
    @NotBlank(message = "Review decision is required")
    String decision, // APPROVED, REJECTED, CORRECTION_REQUESTED
    @NotBlank(message = "Review comments are required")
    String comments,
    Instant reviewDate
) {}

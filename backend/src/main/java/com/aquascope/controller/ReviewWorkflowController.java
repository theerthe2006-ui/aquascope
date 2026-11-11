package com.aquascope.controller;

import com.aquascope.dto.AnalysisReviewDto;
import com.aquascope.dto.ApiResponse;
import com.aquascope.service.ReviewWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Review Workflow", description = "Endpoints for scientific peer review and quality assurance decisions")
public class ReviewWorkflowController {

    private final ReviewWorkflowService reviewService;

    public ReviewWorkflowController(ReviewWorkflowService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/analysis/{analysisId}")
    @Operation(summary = "Get review history for a laboratory analysis")
    public ResponseEntity<ApiResponse<List<AnalysisReviewDto>>> getReviewsForAnalysis(@PathVariable Long analysisId) {
        List<AnalysisReviewDto> reviews = reviewService.getReviewsForAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(reviews, "Analysis reviews retrieved"));
    }

    @PostMapping("/analysis/{analysisId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'REVIEWER')")
    @Operation(summary = "Submit peer review decision (APPROVED, REJECTED, or CORRECTION_REQUESTED)")
    public ResponseEntity<ApiResponse<AnalysisReviewDto>> submitReview(
            @PathVariable Long analysisId,
            @Valid @RequestBody AnalysisReviewDto dto
    ) {
        AnalysisReviewDto saved = reviewService.submitReview(analysisId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(saved, "Review decision recorded successfully"));
    }
}

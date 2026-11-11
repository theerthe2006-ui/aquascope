package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.ParticleObservationDto;
import com.aquascope.dto.SampleAnalysisDto;
import com.aquascope.entity.AnalysisStatus;
import com.aquascope.entity.DataOrigin;
import com.aquascope.service.SampleAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analyses")
@Tag(name = "Laboratory Analyses", description = "Endpoints for microplastic sample spectroscopic and microscopy analysis")
public class SampleAnalysisController {

    private final SampleAnalysisService analysisService;

    public SampleAnalysisController(SampleAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping
    @Operation(summary = "Get all laboratory analyses as a flat list")
    public ResponseEntity<ApiResponse<List<SampleAnalysisDto>>> getAllAnalyses() {
        List<SampleAnalysisDto> list = analysisService.getAllAnalyses();
        return ResponseEntity.ok(ApiResponse.ok(list, "Analyses retrieved"));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated, filtered laboratory analyses")
    public ResponseEntity<ApiResponse<Page<SampleAnalysisDto>>> getAnalysesPage(
            @RequestParam(required = false) AnalysisStatus status,
            @RequestParam(required = false) DataOrigin dataOrigin,
            @PageableDefault(size = 10, sort = "analysisDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SampleAnalysisDto> page = analysisService.getAnalysesPage(status, dataOrigin, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Analyses page retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get analysis details including particle observations")
    public ResponseEntity<ApiResponse<SampleAnalysisDto>> getAnalysisById(@PathVariable Long id) {
        SampleAnalysisDto dto = analysisService.getAnalysisById(id);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Analysis details retrieved"));
    }

    @GetMapping("/sample/{sampleId}")
    @Operation(summary = "Get analyses performed on a specific water sample")
    public ResponseEntity<ApiResponse<List<SampleAnalysisDto>>> getBySampleId(@PathVariable Long sampleId) {
        List<SampleAnalysisDto> list = analysisService.getAnalysesBySampleId(sampleId);
        return ResponseEntity.ok(ApiResponse.ok(list, "Sample analyses retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Create an analysis session for a registered water sample")
    public ResponseEntity<ApiResponse<SampleAnalysisDto>> createAnalysis(@Valid @RequestBody SampleAnalysisDto dto) {
        SampleAnalysisDto created = analysisService.createAnalysis(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Analysis session created successfully"));
    }

    @PostMapping("/{id}/observations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Record a particle observation for an analysis session")
    public ResponseEntity<ApiResponse<ParticleObservationDto>> addObservation(
            @PathVariable Long id,
            @Valid @RequestBody ParticleObservationDto obsDto
    ) {
        ParticleObservationDto added = analysisService.addObservation(id, obsDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(added, "Particle observation recorded successfully"));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Submit completed analysis for peer review")
    public ResponseEntity<ApiResponse<SampleAnalysisDto>> submitForReview(@PathVariable Long id) {
        SampleAnalysisDto submitted = analysisService.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.ok(submitted, "Analysis submitted for review successfully"));
    }
}

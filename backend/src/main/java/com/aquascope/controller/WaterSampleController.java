package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.WaterSampleDto;
import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.SampleStatus;
import com.aquascope.service.WaterSampleService;
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
@RequestMapping("/api/v1/water-samples")
@Tag(name = "Water Samples", description = "Endpoints for environmental water sample collection and registry")
public class WaterSampleController {

    private final WaterSampleService sampleService;

    public WaterSampleController(WaterSampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping
    @Operation(summary = "Get all water samples as a flat list")
    public ResponseEntity<ApiResponse<List<WaterSampleDto>>> getAllSamples() {
        List<WaterSampleDto> samples = sampleService.getAllSamples();
        return ResponseEntity.ok(ApiResponse.ok(samples, "Water samples retrieved"));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated, searchable, and filtered water samples")
    public ResponseEntity<ApiResponse<Page<WaterSampleDto>>> getSamplesPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) SampleStatus status,
            @RequestParam(required = false) DataOrigin dataOrigin,
            @PageableDefault(size = 10, sort = "collectionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<WaterSampleDto> page = sampleService.getSamplesPage(search, siteId, status, dataOrigin, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Water samples page retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get water sample details by ID")
    public ResponseEntity<ApiResponse<WaterSampleDto>> getSampleById(@PathVariable Long id) {
        WaterSampleDto sample = sampleService.getSampleById(id);
        return ResponseEntity.ok(ApiResponse.ok(sample, "Water sample details retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Register a newly collected water sample")
    public ResponseEntity<ApiResponse<WaterSampleDto>> registerSample(@Valid @RequestBody WaterSampleDto dto) {
        WaterSampleDto created = sampleService.registerSample(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Water sample registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Update water sample details")
    public ResponseEntity<ApiResponse<WaterSampleDto>> updateSample(
            @PathVariable Long id,
            @Valid @RequestBody WaterSampleDto dto
    ) {
        WaterSampleDto updated = sampleService.updateSample(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Water sample updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Delete a water sample (Administrator only)")
    public ResponseEntity<ApiResponse<Void>> deleteSample(@PathVariable Long id) {
        sampleService.deleteSample(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Water sample deleted successfully"));
    }
}

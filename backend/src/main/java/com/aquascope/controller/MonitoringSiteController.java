package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.MonitoringSiteDto;
import com.aquascope.entity.WaterSourceCategory;
import com.aquascope.service.MonitoringSiteService;
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
@RequestMapping("/api/v1/monitoring-sites")
@Tag(name = "Monitoring Sites", description = "Endpoints for environmental monitoring stations and water bodies")
public class MonitoringSiteController {

    private final MonitoringSiteService siteService;

    public MonitoringSiteController(MonitoringSiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    @Operation(summary = "Get all monitoring sites as a flat list")
    public ResponseEntity<ApiResponse<List<MonitoringSiteDto>>> getAllSites() {
        List<MonitoringSiteDto> sites = siteService.getAllSites();
        return ResponseEntity.ok(ApiResponse.ok(sites, "Monitoring sites retrieved"));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated, filtered, and searchable monitoring sites")
    public ResponseEntity<ApiResponse<Page<MonitoringSiteDto>>> getSitesPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WaterSourceCategory category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MonitoringSiteDto> page = siteService.getSitesPage(search, category, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Monitoring sites page retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get monitoring site details by ID")
    public ResponseEntity<ApiResponse<MonitoringSiteDto>> getSiteById(@PathVariable Long id) {
        MonitoringSiteDto site = siteService.getSiteById(id);
        return ResponseEntity.ok(ApiResponse.ok(site, "Monitoring site details retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER')")
    @Operation(summary = "Create a new monitoring station")
    public ResponseEntity<ApiResponse<MonitoringSiteDto>> createSite(@Valid @RequestBody MonitoringSiteDto dto) {
        MonitoringSiteDto created = siteService.createSite(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Monitoring site created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER')")
    @Operation(summary = "Update an existing monitoring station")
    public ResponseEntity<ApiResponse<MonitoringSiteDto>> updateSite(
            @PathVariable Long id,
            @Valid @RequestBody MonitoringSiteDto dto
    ) {
        MonitoringSiteDto updated = siteService.updateSite(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Monitoring site updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Delete a monitoring station (Administrator only)")
    public ResponseEntity<ApiResponse<Void>> deleteSite(@PathVariable Long id) {
        siteService.deleteSite(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Monitoring site deleted successfully"));
    }
}

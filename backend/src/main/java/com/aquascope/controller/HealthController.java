package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.HealthResponse;
import com.aquascope.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse health = healthService.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success("Health status retrieved successfully", health));
    }
}

package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.DeviceDto;
import com.aquascope.dto.SensorReadingDto;
import com.aquascope.service.DeviceService;
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
@RequestMapping("/api/v1/devices")
@Tag(name = "Hardware Devices & Sensors", description = "Endpoints for IoT telemetry, autosamplers, and multiparameter water sondes")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @Operation(summary = "List all registered monitoring devices and sensors")
    public ResponseEntity<ApiResponse<List<DeviceDto>>> getAllDevices() {
        List<DeviceDto> list = deviceService.getAllDevices();
        return ResponseEntity.ok(ApiResponse.ok(list, "Devices retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get device details by ID")
    public ResponseEntity<ApiResponse<DeviceDto>> getDeviceById(@PathVariable Long id) {
        DeviceDto dto = deviceService.getDeviceById(id);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Device details retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER')")
    @Operation(summary = "Register a new hardware device or optical sensor")
    public ResponseEntity<ApiResponse<DeviceDto>> registerDevice(@Valid @RequestBody DeviceDto dto) {
        DeviceDto created = deviceService.registerDevice(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Device registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER')")
    @Operation(summary = "Update hardware device configuration")
    public ResponseEntity<ApiResponse<DeviceDto>> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceDto dto
    ) {
        DeviceDto updated = deviceService.updateDevice(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Device updated successfully"));
    }

    @PostMapping("/readings")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER', 'LABORATORY_RESEARCHER')")
    @Operation(summary = "Ingest a sensor reading telemetry packet (Note: Turbidity/pH are auxiliary and do not count particles)")
    public ResponseEntity<ApiResponse<SensorReadingDto>> ingestSensorReading(@Valid @RequestBody SensorReadingDto dto) {
        SensorReadingDto created = deviceService.recordSensorReading(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Sensor reading ingested successfully"));
    }

    @GetMapping("/{id}/readings")
    @Operation(summary = "Get recent sensor readings for a specific device")
    public ResponseEntity<ApiResponse<List<SensorReadingDto>>> getDeviceReadings(@PathVariable Long id) {
        List<SensorReadingDto> readings = deviceService.getLatestReadingsByDevice(id);
        return ResponseEntity.ok(ApiResponse.ok(readings, "Sensor readings retrieved"));
    }

    @GetMapping("/{id}/readings/page")
    @Operation(summary = "Get paginated sensor readings for a device")
    public ResponseEntity<ApiResponse<Page<SensorReadingDto>>> getDeviceReadingsPage(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "readingTimestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SensorReadingDto> page = deviceService.getReadingsPage(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Sensor readings page retrieved"));
    }
}

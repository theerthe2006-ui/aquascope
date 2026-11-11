package com.aquascope.dto;

import com.aquascope.entity.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record DeviceDto(
    Long id,
    @NotBlank(message = "Device identifier is required")
    String deviceIdentifier,
    @NotBlank(message = "Device name is required")
    String name,
    @NotBlank(message = "Device type is required")
    String deviceType,
    Long monitoringSiteId,
    String monitoringSiteCode,
    String manufacturer,
    String firmwareVersion,
    DeviceStatus status,
    Instant lastPing,
    Instant createdAt
) {}

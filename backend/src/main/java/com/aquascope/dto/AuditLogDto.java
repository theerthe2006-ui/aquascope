package com.aquascope.dto;

import com.aquascope.entity.DataOrigin;
import java.time.Instant;

public record AuditLogDto(
    Long id,
    Long userId,
    String username,
    String action,
    String entityType,
    String entityId,
    DataOrigin dataOrigin,
    String details,
    String ipAddress,
    Instant createdAt
) {}

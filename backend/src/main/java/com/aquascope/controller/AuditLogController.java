package com.aquascope.controller;

import com.aquascope.dto.ApiResponse;
import com.aquascope.dto.AuditLogDto;
import com.aquascope.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Logs", description = "Endpoints for immutable traceability, data provenance and regulatory inspection")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get paginated audit logs (Administrator only)")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogDto> page = auditLogService.getAuditLogsPage(pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Audit logs page retrieved"));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'REVIEWER')")
    @Operation(summary = "Get recent audit activity records")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getRecentAuditLogs(
            @RequestParam(defaultValue = "15") int limit
    ) {
        List<AuditLogDto> list = auditLogService.getRecentLogs(limit);
        return ResponseEntity.ok(ApiResponse.ok(list, "Recent audit events retrieved"));
    }
}

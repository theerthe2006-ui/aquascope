package com.aquascope.service;

import com.aquascope.dto.AuditLogDto;
import com.aquascope.entity.AuditLog;
import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.User;
import com.aquascope.repository.AuditLogRepository;
import com.aquascope.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordAction(String action, String entityType, String entityId, String details, DataOrigin origin, String ipAddress) {
        String username = "SYSTEM";
        User user = null;

        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
            user = userRepository.findByUsername(username).orElse(null);
        }

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setDataOrigin(origin != null ? origin : DataOrigin.MANUAL);
        log.setIpAddress(ipAddress);
        log.setUser(user);
        log.setUsername(username);
        log.setCreatedAt(Instant.now());

        auditLogRepository.save(log);
    }

    public Page<AuditLogDto> getAuditLogsPage(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDto);
    }

    public List<AuditLogDto> getRecentLogs(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(limit))
                .getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog a) {
        return new AuditLogDto(
                a.getId(),
                a.getUser() != null ? a.getUser().getId() : null,
                a.getUsername(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDataOrigin(),
                a.getDetails(),
                a.getIpAddress(),
                a.getCreatedAt()
        );
    }
}

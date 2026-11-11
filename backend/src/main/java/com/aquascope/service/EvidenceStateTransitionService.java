package com.aquascope.service;

import com.aquascope.entity.AuditLog;
import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.SampleImages;
import com.aquascope.entity.User;
import com.aquascope.repository.AuditLogRepository;
import com.aquascope.repository.SampleImagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EvidenceStateTransitionService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceStateTransitionService.class);

    private final SampleImagesRepository sampleImagesRepository;
    private final AuditLogRepository auditLogRepository;

    public EvidenceStateTransitionService(
            SampleImagesRepository sampleImagesRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.sampleImagesRepository = sampleImagesRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Mark evidence as PENDING_DELETION in a dedicated, independently committed transaction.
     * Prevents race conditions and guarantees that subsequent storage failures retain this state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SampleImages markPendingDeletion(Long imageId, String initiatedByUsername) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found with ID: " + imageId));

        // If already pending deletion, allow idempotent continuation
        if (!"PENDING_DELETION".equals(image.getStorageStatus())) {
            image.setStorageStatus("PENDING_DELETION");
            image = sampleImagesRepository.saveAndFlush(image);
            log.info("Evidence ID {} marked as PENDING_DELETION by user {}", imageId, initiatedByUsername);

            // Record audit log for deletion initiation
            AuditLog audit = new AuditLog();
            audit.setAction("EVIDENCE_DELETION_INITIATED");
            audit.setEntityType("SampleImages");
            audit.setEntityId(String.valueOf(imageId));
            audit.setUsername(initiatedByUsername);
            audit.setDataOrigin(DataOrigin.MANUAL);
            audit.setDetails(String.format("Deletion initiated for file: %s (Key: %s, SHA-256: %s)",
                    image.getFileName(), image.getObjectKey(), image.getSha256Checksum()));
            auditLogRepository.saveAndFlush(audit);
        }

        return image;
    }

    /**
     * Finalize deletion after verified removal from storage in a dedicated committed transaction.
     * Preserves an immutable audit record with file metadata even after the row is removed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeDeletion(Long imageId, String completedByUsername) {
        SampleImages image = sampleImagesRepository.findById(imageId).orElse(null);
        if (image != null) {
            // Write permanent deletion audit log before removing entity
            AuditLog audit = new AuditLog();
            audit.setAction("EVIDENCE_DELETED_PERMANENTLY");
            audit.setEntityType("SampleImages");
            audit.setEntityId(String.valueOf(imageId));
            audit.setUsername(completedByUsername);
            audit.setDataOrigin(DataOrigin.MANUAL);
            audit.setDetails(String.format("Permanently removed evidence file: %s (Key: %s, Checksum: %s, Size: %d bytes)",
                    image.getFileName(), image.getObjectKey(), image.getSha256Checksum(), image.getFileSizeBytes()));
            auditLogRepository.saveAndFlush(audit);

            image.setStorageStatus("DELETED");
            sampleImagesRepository.saveAndFlush(image);
            sampleImagesRepository.delete(image);
            log.info("Evidence ID {} successfully purged and permanently finalized in database", imageId);
        }
    }

    /**
     * Mark an image as STORAGE_ERROR if an unexpected storage anomaly occurred.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStorageError(Long imageId, String reason) {
        SampleImages image = sampleImagesRepository.findById(imageId).orElse(null);
        if (image != null) {
            image.setStorageStatus("STORAGE_ERROR");
            sampleImagesRepository.saveAndFlush(image);

            AuditLog audit = new AuditLog();
            audit.setAction("EVIDENCE_STORAGE_ERROR");
            audit.setEntityType("SampleImages");
            audit.setEntityId(String.valueOf(imageId));
            audit.setDataOrigin(DataOrigin.MANUAL);
            audit.setDetails("Storage operation failed: " + reason);
            auditLogRepository.saveAndFlush(audit);

            log.warn("Evidence ID {} marked as STORAGE_ERROR: {}", imageId, reason);
        }
    }
}

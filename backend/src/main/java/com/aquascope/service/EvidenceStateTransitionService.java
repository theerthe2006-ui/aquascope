package com.aquascope.service;

import com.aquascope.entity.SampleImages;
import com.aquascope.repository.SampleImagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceStateTransitionService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceStateTransitionService.class);

    private final SampleImagesRepository sampleImagesRepository;

    public EvidenceStateTransitionService(SampleImagesRepository sampleImagesRepository) {
        this.sampleImagesRepository = sampleImagesRepository;
    }

    /**
     * Mark evidence as PENDING_DELETION in a dedicated, independent committed transaction.
     * Even if subsequent external storage calls fail, this database state is firmly committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SampleImages markPendingDeletion(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found with ID: " + imageId));

        image.setStorageStatus("PENDING_DELETION");
        SampleImages saved = sampleImagesRepository.saveAndFlush(image);
        log.info("Evidence ID {} marked as PENDING_DELETION in autonomous transaction", imageId);
        return saved;
    }

    /**
     * Finalize deletion after verified removal from storage in a dedicated committed transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeDeletion(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId).orElse(null);
        if (image != null) {
            image.setStorageStatus("DELETED");
            sampleImagesRepository.saveAndFlush(image);
            sampleImagesRepository.delete(image);
            log.info("Evidence ID {} permanently finalized and removed from database", imageId);
        }
    }

    /**
     * Mark an image as STORAGE_ERROR if an unexpected storage anomaly occurred.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStorageError(Long imageId) {
        SampleImages image = sampleImagesRepository.findById(imageId).orElse(null);
        if (image != null) {
            image.setStorageStatus("STORAGE_ERROR");
            sampleImagesRepository.saveAndFlush(image);
            log.warn("Evidence ID {} marked as STORAGE_ERROR in autonomous transaction", imageId);
        }
    }
}

package com.aquascope.service;

import com.aquascope.dto.AnalysisReviewDto;
import com.aquascope.entity.*;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.AnalysisReviewRepository;
import com.aquascope.repository.AuditLogRepository;
import com.aquascope.repository.SampleAnalysisRepository;
import com.aquascope.repository.UserRepository;
import com.aquascope.repository.WaterSampleRepository;
import com.aquascope.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewWorkflowService {

    private final AnalysisReviewRepository reviewRepository;
    private final SampleAnalysisRepository analysisRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public ReviewWorkflowService(
            AnalysisReviewRepository reviewRepository,
            SampleAnalysisRepository analysisRepository,
            WaterSampleRepository waterSampleRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AnalysisReviewDto> getReviewsForAnalysis(Long analysisId) {
        return reviewRepository.findBySampleAnalysisId(analysisId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnalysisReviewDto submitReview(Long analysisId, AnalysisReviewDto dto) {
        SampleAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));

        User currentUser = getCurrentUser();

        if (currentUser != null && analysis.getAnalystUser() != null) {
            if (currentUser.getId().equals(analysis.getAnalystUser().getId()) && currentUser.getRole() != Role.ADMINISTRATOR) {
                throw new AccessDeniedException("Reviewer Conflict of Interest: A researcher cannot approve or review their own laboratory analysis.");
            }
        }

        String decision = dto.decision().trim().toUpperCase();
        AnalysisStatus newStatus;
        switch (decision) {
            case "APPROVED" -> newStatus = AnalysisStatus.APPROVED;
            case "REJECTED" -> newStatus = AnalysisStatus.REJECTED;
            case "CORRECTION_REQUESTED" -> newStatus = AnalysisStatus.DRAFT;
            default -> throw new IllegalArgumentException("Invalid review decision: '" + dto.decision() + "'. Expected APPROVED, REJECTED, or CORRECTION_REQUESTED");
        }

        AnalysisReview review = new AnalysisReview();
        review.setSampleAnalysis(analysis);
        review.setReviewerUser(currentUser);
        review.setDecision(decision);
        review.setComments(dto.comments());
        review.setReviewDate(Instant.now());

        AnalysisReview saved = reviewRepository.save(review);

        analysis.setStatus(newStatus);
        analysis.setUpdatedAt(Instant.now());
        analysisRepository.save(analysis);

        // Synchronize parent WaterSample status
        if (analysis.getWaterSample() != null) {
            WaterSample sample = analysis.getWaterSample();
            if (newStatus == AnalysisStatus.APPROVED) {
                sample.setStatus(SampleStatus.APPROVED);
            } else if (newStatus == AnalysisStatus.REJECTED) {
                sample.setStatus(SampleStatus.REJECTED);
            } else if (newStatus == AnalysisStatus.DRAFT) {
                sample.setStatus(SampleStatus.IN_ANALYSIS);
            }
            sample.setUpdatedAt(Instant.now());
            waterSampleRepository.save(sample);
        }

        AuditLog log = new AuditLog();
        log.setEntityType("SAMPLE_ANALYSIS");
        log.setEntityId(String.valueOf(analysis.getId()));
        log.setAction("REVIEW_" + decision);
        log.setUser(currentUser);
        log.setDetails("Peer review completed with decision: " + decision + ". Comments: " + dto.comments());
        auditLogRepository.save(log);

        return toDto(saved);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private AnalysisReviewDto toDto(AnalysisReview r) {
        return new AnalysisReviewDto(
                r.getId(),
                r.getSampleAnalysis() != null ? r.getSampleAnalysis().getId() : null,
                r.getReviewerUser() != null ? r.getReviewerUser().getUsername() : null,
                r.getDecision(),
                r.getComments(),
                r.getReviewDate()
        );
    }
}

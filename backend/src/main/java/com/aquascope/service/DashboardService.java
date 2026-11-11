package com.aquascope.service;

import com.aquascope.dto.DashboardStatsDto;
import com.aquascope.entity.*;
import com.aquascope.repository.MonitoringSiteRepository;
import com.aquascope.repository.ParticleObservationRepository;
import com.aquascope.repository.SampleAnalysisRepository;
import com.aquascope.repository.WaterSampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final MonitoringSiteRepository siteRepository;
    private final WaterSampleRepository sampleRepository;
    private final SampleAnalysisRepository analysisRepository;
    private final ParticleObservationRepository observationRepository;

    public DashboardService(
            MonitoringSiteRepository siteRepository,
            WaterSampleRepository sampleRepository,
            SampleAnalysisRepository analysisRepository,
            ParticleObservationRepository observationRepository
    ) {
        this.siteRepository = siteRepository;
        this.sampleRepository = sampleRepository;
        this.analysisRepository = analysisRepository;
        this.observationRepository = observationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalSites = siteRepository.count();
        long totalSamples = sampleRepository.count();

        List<SampleAnalysis> allAnalyses = analysisRepository.findAll();
        long pendingAnalyses = allAnalyses.stream().filter(a -> a.getStatus() == AnalysisStatus.DRAFT).count();
        long completedAnalyses = allAnalyses.stream().filter(a -> a.getStatus() == AnalysisStatus.COMPLETED).count();
        long pendingReviews = allAnalyses.stream().filter(a -> a.getStatus() == AnalysisStatus.SUBMITTED_FOR_REVIEW).count();
        long approvedAnalyses = allAnalyses.stream().filter(a -> a.getStatus() == AnalysisStatus.APPROVED).count();
        long rejectedAnalyses = allAnalyses.stream().filter(a -> a.getStatus() == AnalysisStatus.REJECTED).count();

        List<ParticleObservation> allObservations = observationRepository.findAll();
        long totalParticles = allObservations.stream().mapToLong(ParticleObservation::getParticleCount).sum();

        // Data origin distribution across samples
        Map<DataOrigin, Long> originMap = new HashMap<>();
        for (DataOrigin origin : DataOrigin.values()) {
            originMap.put(origin, sampleRepository.countByDataOrigin(origin));
        }

        // Particle Shape distribution
        Map<ParticleShape, Long> shapeMap = new HashMap<>();
        for (ParticleShape shape : ParticleShape.values()) {
            shapeMap.put(shape, observationRepository.countByShape(shape));
        }

        // Polymer Type distribution
        Map<PolymerType, Long> polymerMap = new HashMap<>();
        for (PolymerType type : PolymerType.values()) {
            polymerMap.put(type, observationRepository.countByPolymerType(type));
        }

        return new DashboardStatsDto(
                totalSites,
                totalSamples,
                pendingAnalyses,
                completedAnalyses,
                pendingReviews,
                approvedAnalyses,
                rejectedAnalyses,
                totalParticles,
                originMap,
                shapeMap,
                polymerMap
        );
    }
}

package com.aquascope.dto;

import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.ParticleShape;
import com.aquascope.entity.PolymerType;

import java.util.Map;

public record DashboardStatsDto(
        long totalMonitoringSites,
        long totalWaterSamples,
        long pendingAnalyses,
        long completedAnalyses,
        long pendingReviews,
        long approvedAnalyses,
        long rejectedAnalyses,
        long totalParticleObservations,
        Map<DataOrigin, Long> dataOriginDistribution,
        Map<ParticleShape, Long> particleShapeDistribution,
        Map<PolymerType, Long> polymerTypeDistribution
) {}

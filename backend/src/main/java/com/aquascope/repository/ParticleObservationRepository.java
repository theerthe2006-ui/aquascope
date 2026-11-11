package com.aquascope.repository;

import com.aquascope.entity.ParticleObservation;
import com.aquascope.entity.ParticleShape;
import com.aquascope.entity.PolymerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ParticleObservationRepository extends JpaRepository<ParticleObservation, Long> {
    List<ParticleObservation> findBySampleAnalysisId(Long sampleAnalysisId);

    long countByShape(ParticleShape shape);
    long countByPolymerType(PolymerType polymerType);

    @Query("SELECT p.shape as shape, COUNT(p) as count, SUM(p.particleCount) as totalParticles " +
           "FROM ParticleObservation p GROUP BY p.shape")
    List<Map<String, Object>> countParticlesByShape();

    @Query("SELECT p.polymerType as polymerType, COUNT(p) as count " +
           "FROM ParticleObservation p GROUP BY p.polymerType")
    List<Map<String, Object>> countParticlesByPolymer();
}

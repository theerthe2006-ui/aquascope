package com.aquascope.repository;

import com.aquascope.entity.AnalysisStatus;
import com.aquascope.entity.SampleAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SampleAnalysisRepository extends JpaRepository<SampleAnalysis, Long> {
    List<SampleAnalysis> findByWaterSampleId(Long waterSampleId);
    Page<SampleAnalysis> findByStatus(AnalysisStatus status, Pageable pageable);
    long countByStatus(AnalysisStatus status);
}

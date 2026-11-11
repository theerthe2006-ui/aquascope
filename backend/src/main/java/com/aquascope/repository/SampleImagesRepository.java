package com.aquascope.repository;

import com.aquascope.entity.SampleImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SampleImagesRepository extends JpaRepository<SampleImages, Long> {
    List<SampleImages> findByWaterSampleId(Long waterSampleId);
    List<SampleImages> findBySampleAnalysisId(Long sampleAnalysisId);
}

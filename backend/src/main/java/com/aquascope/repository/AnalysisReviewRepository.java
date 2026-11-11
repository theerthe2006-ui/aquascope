package com.aquascope.repository;

import com.aquascope.entity.AnalysisReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisReviewRepository extends JpaRepository<AnalysisReview, Long> {
    List<AnalysisReview> findBySampleAnalysisId(Long sampleAnalysisId);
}

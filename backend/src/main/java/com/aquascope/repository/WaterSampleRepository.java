package com.aquascope.repository;

import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.SampleStatus;
import com.aquascope.entity.WaterSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaterSampleRepository extends JpaRepository<WaterSample, Long> {
    Optional<WaterSample> findBySampleIdentifier(String sampleIdentifier);
    boolean existsBySampleIdentifier(String sampleIdentifier);

    Page<WaterSample> findBySampleIdentifierContainingIgnoreCase(String identifier, Pageable pageable);
    Page<WaterSample> findByMonitoringSiteId(Long siteId, Pageable pageable);
    Page<WaterSample> findByStatus(SampleStatus status, Pageable pageable);
    Page<WaterSample> findByDataOrigin(DataOrigin dataOrigin, Pageable pageable);

    @Query("SELECT s FROM WaterSample s WHERE " +
           "(:siteId IS NULL OR s.monitoringSite.id = :siteId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:query IS NULL OR LOWER(s.sampleIdentifier) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.collectionSource) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<WaterSample> searchSamples(
            @Param("siteId") Long siteId,
            @Param("status") SampleStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    long countByStatus(SampleStatus status);
    long countByDataOrigin(DataOrigin dataOrigin);
}

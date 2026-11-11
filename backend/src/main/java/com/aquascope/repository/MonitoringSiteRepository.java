package com.aquascope.repository;

import com.aquascope.entity.MonitoringSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringSiteRepository extends JpaRepository<MonitoringSite, Long> {
    Optional<MonitoringSite> findByCode(String code);
    boolean existsByCode(String code);

    @Query("SELECT s FROM MonitoringSite s WHERE " +
           "(:query IS NULL OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<MonitoringSite> searchSites(@Param("query") String query, Pageable pageable);

    List<MonitoringSite> findByActiveTrue();
}

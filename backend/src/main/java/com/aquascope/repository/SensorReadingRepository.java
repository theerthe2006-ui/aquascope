package com.aquascope.repository;

import com.aquascope.entity.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findByDeviceIdOrderByReadingTimestampDesc(Long deviceId);
    Page<SensorReading> findByDeviceId(Long deviceId, Pageable pageable);
}

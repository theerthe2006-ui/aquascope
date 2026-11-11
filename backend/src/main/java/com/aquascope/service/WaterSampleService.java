package com.aquascope.service;

import com.aquascope.dto.WaterSampleDto;
import com.aquascope.entity.*;
import com.aquascope.exception.DuplicateResourceException;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.MonitoringSiteRepository;
import com.aquascope.repository.UserRepository;
import com.aquascope.repository.WaterSampleRepository;
import com.aquascope.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WaterSampleService {

    private final WaterSampleRepository waterSampleRepository;
    private final MonitoringSiteRepository monitoringSiteRepository;
    private final UserRepository userRepository;

    public WaterSampleService(
            WaterSampleRepository waterSampleRepository,
            MonitoringSiteRepository monitoringSiteRepository,
            UserRepository userRepository
    ) {
        this.waterSampleRepository = waterSampleRepository;
        this.monitoringSiteRepository = monitoringSiteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WaterSampleDto> getAllSamples() {
        return waterSampleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WaterSampleDto> getSamplesPage(
            String search,
            Long siteId,
            SampleStatus status,
            DataOrigin origin,
            Pageable pageable
    ) {
        Page<WaterSample> samples;
        if (search != null && !search.isBlank()) {
            samples = waterSampleRepository.findBySampleIdentifierContainingIgnoreCase(search.trim(), pageable);
        } else if (siteId != null) {
            samples = waterSampleRepository.findByMonitoringSiteId(siteId, pageable);
        } else if (status != null) {
            samples = waterSampleRepository.findByStatus(status, pageable);
        } else if (origin != null) {
            samples = waterSampleRepository.findByDataOrigin(origin, pageable);
        } else {
            samples = waterSampleRepository.findAll(pageable);
        }
        return samples.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WaterSampleDto getSampleById(Long id) {
        WaterSample sample = waterSampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Water sample not found with id: " + id));
        return toDto(sample);
    }

    @Transactional
    public WaterSampleDto registerSample(WaterSampleDto dto) {
        String identifier = dto.sampleIdentifier();
        if (identifier == null || identifier.isBlank()) {
            identifier = "SMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        if (waterSampleRepository.existsBySampleIdentifier(identifier)) {
            throw new DuplicateResourceException("Sample identifier '" + identifier + "' is already registered");
        }

        MonitoringSite site = monitoringSiteRepository.findById(dto.monitoringSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found with id: " + dto.monitoringSiteId()));

        User currentUser = getCurrentUser();

        WaterSample sample = new WaterSample();
        sample.setSampleIdentifier(identifier);
        sample.setMonitoringSite(site);
        sample.setCollectedByUser(currentUser);
        sample.setCollectionDate(dto.collectionDate() != null ? dto.collectionDate() : Instant.now());
        sample.setSampleVolume(dto.sampleVolume());
        sample.setVolumeUnit(dto.volumeUnit() != null ? dto.volumeUnit() : "L");
        sample.setSamplingDepthMeters(dto.samplingDepthMeters());
        sample.setWaterTemperatureCelsius(dto.waterTemperatureCelsius());
        sample.setPhLevel(dto.phLevel());
        sample.setTurbidityNtu(dto.turbidityNtu());
        sample.setCollectionSource(dto.collectionSource());
        sample.setWeatherConditions(dto.weatherConditions());
        sample.setDataOrigin(dto.dataOrigin() != null ? dto.dataOrigin() : DataOrigin.MANUAL);
        sample.setStatus(dto.status() != null ? dto.status() : SampleStatus.REGISTERED);
        sample.setNotes(dto.notes());

        WaterSample saved = waterSampleRepository.save(sample);
        return toDto(saved);
    }

    @Transactional
    public WaterSampleDto updateSample(Long id, WaterSampleDto dto) {
        WaterSample sample = waterSampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Water sample not found with id: " + id));

        if (!sample.getSampleIdentifier().equalsIgnoreCase(dto.sampleIdentifier()) &&
                waterSampleRepository.existsBySampleIdentifier(dto.sampleIdentifier())) {
            throw new DuplicateResourceException("Sample identifier '" + dto.sampleIdentifier() + "' already exists");
        }

        if (dto.monitoringSiteId() != null && !dto.monitoringSiteId().equals(sample.getMonitoringSite().getId())) {
            MonitoringSite site = monitoringSiteRepository.findById(dto.monitoringSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found with id: " + dto.monitoringSiteId()));
            sample.setMonitoringSite(site);
        }

        sample.setSampleIdentifier(dto.sampleIdentifier());
        sample.setCollectionDate(dto.collectionDate());
        sample.setSampleVolume(dto.sampleVolume());
        sample.setVolumeUnit(dto.volumeUnit());
        sample.setSamplingDepthMeters(dto.samplingDepthMeters());
        sample.setWaterTemperatureCelsius(dto.waterTemperatureCelsius());
        sample.setPhLevel(dto.phLevel());
        sample.setTurbidityNtu(dto.turbidityNtu());
        sample.setCollectionSource(dto.collectionSource());
        sample.setWeatherConditions(dto.weatherConditions());
        if (dto.dataOrigin() != null) sample.setDataOrigin(dto.dataOrigin());
        if (dto.status() != null) sample.setStatus(dto.status());
        sample.setNotes(dto.notes());
        sample.setUpdatedAt(Instant.now());

        return toDto(waterSampleRepository.save(sample));
    }

    @Transactional
    public void deleteSample(Long id) {
        WaterSample sample = waterSampleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Water sample not found with id: " + id));
        waterSampleRepository.delete(sample);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private WaterSampleDto toDto(WaterSample s) {
        return new WaterSampleDto(
                s.getId(),
                s.getSampleIdentifier(),
                s.getMonitoringSite() != null ? s.getMonitoringSite().getId() : null,
                s.getMonitoringSite() != null ? s.getMonitoringSite().getCode() : null,
                s.getMonitoringSite() != null ? s.getMonitoringSite().getName() : null,
                s.getCollectedByUser() != null ? s.getCollectedByUser().getUsername() : null,
                s.getCollectionDate(),
                s.getSampleVolume(),
                s.getVolumeUnit(),
                s.getSamplingDepthMeters(),
                s.getWaterTemperatureCelsius(),
                s.getPhLevel(),
                s.getTurbidityNtu(),
                s.getCollectionSource(),
                s.getWeatherConditions(),
                s.getDataOrigin(),
                s.getStatus(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }
}

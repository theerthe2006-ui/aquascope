package com.aquascope.service;

import com.aquascope.dto.MonitoringSiteDto;
import com.aquascope.entity.MonitoringSite;
import com.aquascope.entity.User;
import com.aquascope.entity.WaterSourceCategory;
import com.aquascope.exception.DuplicateResourceException;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.MonitoringSiteRepository;
import com.aquascope.repository.UserRepository;
import com.aquascope.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitoringSiteService {

    private final MonitoringSiteRepository monitoringSiteRepository;
    private final UserRepository userRepository;

    public MonitoringSiteService(
            MonitoringSiteRepository monitoringSiteRepository,
            UserRepository userRepository
    ) {
        this.monitoringSiteRepository = monitoringSiteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MonitoringSiteDto> getAllSites() {
        return monitoringSiteRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MonitoringSiteDto> getSitesPage(String search, WaterSourceCategory category, Pageable pageable) {
        Page<MonitoringSite> sites;
        if (search != null && !search.isBlank()) {
            sites = monitoringSiteRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    search.trim(), search.trim(), pageable
            );
        } else if (category != null) {
            sites = monitoringSiteRepository.findByWaterSourceCategory(category, pageable);
        } else {
            sites = monitoringSiteRepository.findAll(pageable);
        }
        return sites.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public MonitoringSiteDto getSiteById(Long id) {
        MonitoringSite site = monitoringSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found with id: " + id));
        return toDto(site);
    }

    @Transactional
    public MonitoringSiteDto createSite(MonitoringSiteDto dto) {
        if (monitoringSiteRepository.existsByCode(dto.code())) {
            throw new DuplicateResourceException("Monitoring site code '" + dto.code() + "' already exists");
        }

        User currentUser = getCurrentUser();

        MonitoringSite site = new MonitoringSite(
                dto.code(),
                dto.name(),
                dto.description(),
                dto.waterSourceCategory(),
                dto.latitude(),
                dto.longitude(),
                dto.elevationMeters(),
                currentUser
        );

        MonitoringSite saved = monitoringSiteRepository.save(site);
        return toDto(saved);
    }

    @Transactional
    public MonitoringSiteDto updateSite(Long id, MonitoringSiteDto dto) {
        MonitoringSite site = monitoringSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found with id: " + id));

        if (!site.getCode().equalsIgnoreCase(dto.code()) && monitoringSiteRepository.existsByCode(dto.code())) {
            throw new DuplicateResourceException("Monitoring site code '" + dto.code() + "' already exists");
        }

        site.setCode(dto.code());
        site.setName(dto.name());
        site.setDescription(dto.description());
        site.setWaterSourceCategory(dto.waterSourceCategory());
        site.setLatitude(dto.latitude());
        site.setLongitude(dto.longitude());
        site.setElevationMeters(dto.elevationMeters());
        site.setActive(dto.active());
        site.setUpdatedAt(Instant.now());

        return toDto(monitoringSiteRepository.save(site));
    }

    @Transactional
    public void deleteSite(Long id) {
        MonitoringSite site = monitoringSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found with id: " + id));
        monitoringSiteRepository.delete(site);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private MonitoringSiteDto toDto(MonitoringSite site) {
        return new MonitoringSiteDto(
                site.getId(),
                site.getCode(),
                site.getName(),
                site.getDescription(),
                site.getWaterSourceCategory(),
                site.getLatitude(),
                site.getLongitude(),
                site.getElevationMeters(),
                site.getCreatedByUser() != null ? site.getCreatedByUser().getUsername() : null,
                site.isActive(),
                site.getCreatedAt()
        );
    }
}

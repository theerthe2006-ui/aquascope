package com.aquascope.service;

import com.aquascope.dto.ParticleObservationDto;
import com.aquascope.dto.SampleAnalysisDto;
import com.aquascope.entity.*;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.*;
import com.aquascope.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SampleAnalysisService {

    private final SampleAnalysisRepository analysisRepository;
    private final ParticleObservationRepository observationRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public SampleAnalysisService(
            SampleAnalysisRepository analysisRepository,
            ParticleObservationRepository observationRepository,
            WaterSampleRepository waterSampleRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.observationRepository = observationRepository;
        this.waterSampleRepository = waterSampleRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<SampleAnalysisDto> getAllAnalyses() {
        return analysisRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SampleAnalysisDto> getAnalysesPage(AnalysisStatus status, DataOrigin origin, Pageable pageable) {
        Page<SampleAnalysis> page;
        if (status != null) {
            page = analysisRepository.findByStatus(status, pageable);
        } else if (origin != null) {
            page = analysisRepository.findByDataOrigin(origin, pageable);
        } else {
            page = analysisRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public SampleAnalysisDto getAnalysisById(Long id) {
        SampleAnalysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + id));
        return toDto(analysis);
    }

    @Transactional(readOnly = true)
    public List<SampleAnalysisDto> getAnalysesBySampleId(Long sampleId) {
        return analysisRepository.findByWaterSampleId(sampleId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SampleAnalysisDto createAnalysis(SampleAnalysisDto dto) {
        WaterSample sample = waterSampleRepository.findById(dto.waterSampleId())
                .orElseThrow(() -> new ResourceNotFoundException("Water sample not found with id: " + dto.waterSampleId()));

        User currentUser = getCurrentUser();

        SampleAnalysis analysis = new SampleAnalysis();
        analysis.setWaterSample(sample);
        analysis.setAnalystUser(currentUser);
        analysis.setAnalysisDate(dto.analysisDate() != null ? dto.analysisDate() : Instant.now());
        analysis.setAnalysisMethod(dto.analysisMethod());
        analysis.setEquipmentModel(dto.equipmentModel());
        analysis.setMagnification(dto.magnification());
        analysis.setPreparationMethod(dto.preparationMethod());
        analysis.setFilterMaterial(dto.filterMaterial());
        analysis.setFilterPoreSizeUm(dto.filterPoreSizeUm());
        analysis.setStatus(AnalysisStatus.DRAFT);
        analysis.setDataOrigin(dto.dataOrigin() != null ? dto.dataOrigin() : DataOrigin.MANUAL);
        analysis.setNotes(dto.notes());

        SampleAnalysis saved = analysisRepository.save(analysis);

        int totalCount = 0;
        if (dto.observations() != null && !dto.observations().isEmpty()) {
            for (ParticleObservationDto obsDto : dto.observations()) {
                ParticleObservation obs = toEntity(obsDto, saved);
                observationRepository.save(obs);
                totalCount += obs.getParticleCount();
            }
        }

        saved.setTotalParticlesCounted(totalCount);
        if (sample.getSampleVolume() != null && sample.getSampleVolume().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal conc = BigDecimal.valueOf(totalCount)
                    .divide(sample.getSampleVolume(), 2, RoundingMode.HALF_UP);
            saved.setConcentrationParticlesPerLiter(conc);
        }

        saved = analysisRepository.save(saved);
        sample.setStatus(SampleStatus.IN_ANALYSIS);
        waterSampleRepository.save(sample);

        logAudit("SAMPLE_ANALYSIS", String.valueOf(saved.getId()), "CREATE", "Laboratory analysis initialized by " + (currentUser != null ? currentUser.getUsername() : "system"));

        return toDto(saved);
    }

    @Transactional
    public ParticleObservationDto addObservation(Long analysisId, ParticleObservationDto obsDto) {
        SampleAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));

        ParticleObservation obs = toEntity(obsDto, analysis);
        ParticleObservation savedObs = observationRepository.save(obs);

        List<ParticleObservation> allObs = observationRepository.findBySampleAnalysisId(analysisId);
        int totalCount = allObs.stream().mapToInt(ParticleObservation::getParticleCount).sum();
        analysis.setTotalParticlesCounted(totalCount);

        WaterSample sample = analysis.getWaterSample();
        if (sample != null && sample.getSampleVolume() != null && sample.getSampleVolume().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal conc = BigDecimal.valueOf(totalCount)
                    .divide(sample.getSampleVolume(), 2, RoundingMode.HALF_UP);
            analysis.setConcentrationParticlesPerLiter(conc);
        }
        analysisRepository.save(analysis);

        return toDto(savedObs);
    }

    @Transactional
    public SampleAnalysisDto submitForReview(Long analysisId) {
        SampleAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));

        analysis.setStatus(AnalysisStatus.SUBMITTED_FOR_REVIEW);
        analysis.setUpdatedAt(Instant.now());
        SampleAnalysis saved = analysisRepository.save(analysis);

        WaterSample sample = analysis.getWaterSample();
        if (sample != null) {
            sample.setStatus(SampleStatus.UNDER_REVIEW);
            waterSampleRepository.save(sample);
        }

        User currentUser = getCurrentUser();
        logAudit("SAMPLE_ANALYSIS", String.valueOf(saved.getId()), "SUBMIT", "Analysis submitted for peer review by " + (currentUser != null ? currentUser.getUsername() : "researcher"));

        return toDto(saved);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private void logAudit(String entityType, String entityId, String action, String details) {
        User user = getCurrentUser();
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setUser(user);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private SampleAnalysisDto toDto(SampleAnalysis a) {
        List<ParticleObservationDto> obsDtos = observationRepository.findBySampleAnalysisId(a.getId())
                .stream().map(this::toDto).collect(Collectors.toList());

        return new SampleAnalysisDto(
                a.getId(),
                a.getWaterSample() != null ? a.getWaterSample().getId() : null,
                a.getWaterSample() != null ? a.getWaterSample().getSampleIdentifier() : null,
                a.getAnalystUser() != null ? a.getAnalystUser().getUsername() : null,
                a.getAnalysisDate(),
                a.getAnalysisMethod(),
                a.getEquipmentModel(),
                a.getMagnification(),
                a.getPreparationMethod(),
                a.getFilterMaterial(),
                a.getFilterPoreSizeUm(),
                a.getTotalParticlesCounted(),
                a.getConcentrationParticlesPerLiter(),
                a.getStatus(),
                a.getDataOrigin(),
                a.getNotes(),
                obsDtos,
                a.getCreatedAt()
        );
    }

    private ParticleObservationDto toDto(ParticleObservation o) {
        return new ParticleObservationDto(
                o.getId(),
                o.getParticleCount(),
                o.getSizeMicrometers(),
                o.getLengthMicrometers(),
                o.getWidthMicrometers(),
                o.getShape(),
                o.getColor(),
                o.getPolymerType(),
                o.getIdentificationMethod(),
                o.getConfidenceLevel(),
                o.getSpectralMatchScore(),
                o.getNotes()
        );
    }

    private ParticleObservation toEntity(ParticleObservationDto d, SampleAnalysis a) {
        ParticleObservation o = new ParticleObservation();
        o.setSampleAnalysis(a);
        o.setParticleCount(d.particleCount() > 0 ? d.particleCount() : 1);
        o.setSizeMicrometers(d.sizeMicrometers());
        o.setLengthMicrometers(d.lengthMicrometers());
        o.setWidthMicrometers(d.widthMicrometers());
        o.setShape(d.shape());
        o.setColor(d.color());
        o.setPolymerType(d.polymerType() != null ? d.polymerType() : PolymerType.UNVERIFIED);
        o.setIdentificationMethod(d.identificationMethod() != null ? d.identificationMethod() : IdentificationMethod.MORPHOLOGICAL_ONLY);
        o.setConfidenceLevel(d.confidenceLevel());
        o.setSpectralMatchScore(d.spectralMatchScore());
        o.setNotes(d.notes());
        return o;
    }
}

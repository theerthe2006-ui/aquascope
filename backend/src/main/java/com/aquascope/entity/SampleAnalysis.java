package com.aquascope.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sample_analyses")
public class SampleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_sample_id", nullable = false)
    private WaterSample waterSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyst_user_id", nullable = false)
    private User analystUser;

    @Column(name = "analysis_date", nullable = false)
    private Instant analysisDate;

    @Column(name = "analysis_method", nullable = false, length = 100)
    private String analysisMethod; // OPTICAL_MICROSCOPY, FTIR_SPECTROSCOPY, etc.

    @Column(name = "equipment_model", length = 200)
    private String equipmentModel;

    @Column(length = 50)
    private String magnification;

    @Column(name = "preparation_method", length = 200)
    private String preparationMethod;

    @Column(name = "filter_material", length = 100)
    private String filterMaterial;

    @Column(name = "filter_pore_size_um", precision = 6, scale = 2)
    private BigDecimal filterPoreSizeUm;

    @Column(name = "total_particles_counted", nullable = false)
    private int totalParticlesCounted = 0;

    @Column(name = "concentration_particles_per_liter", precision = 12, scale = 4)
    private BigDecimal concentrationParticlesPerLiter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnalysisStatus status = AnalysisStatus.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_origin", nullable = false, length = 50)
    private DataOrigin dataOrigin = DataOrigin.MANUAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "sampleAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticleObservation> observations = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public SampleAnalysis() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addObservation(ParticleObservation observation) {
        observations.add(observation);
        observation.setSampleAnalysis(this);
    }

    public void removeObservation(ParticleObservation observation) {
        observations.remove(observation);
        observation.setSampleAnalysis(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WaterSample getWaterSample() { return waterSample; }
    public void setWaterSample(WaterSample waterSample) { this.waterSample = waterSample; }

    public User getAnalystUser() { return analystUser; }
    public void setAnalystUser(User analystUser) { this.analystUser = analystUser; }

    public Instant getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Instant analysisDate) { this.analysisDate = analysisDate; }

    public String getAnalysisMethod() { return analysisMethod; }
    public void setAnalysisMethod(String analysisMethod) { this.analysisMethod = analysisMethod; }

    public String getEquipmentModel() { return equipmentModel; }
    public void setEquipmentModel(String equipmentModel) { this.equipmentModel = equipmentModel; }

    public String getMagnification() { return magnification; }
    public void setMagnification(String magnification) { this.magnification = magnification; }

    public String getPreparationMethod() { return preparationMethod; }
    public void setPreparationMethod(String preparationMethod) { this.preparationMethod = preparationMethod; }

    public String getFilterMaterial() { return filterMaterial; }
    public void setFilterMaterial(String filterMaterial) { this.filterMaterial = filterMaterial; }

    public BigDecimal getFilterPoreSizeUm() { return filterPoreSizeUm; }
    public void setFilterPoreSizeUm(BigDecimal filterPoreSizeUm) { this.filterPoreSizeUm = filterPoreSizeUm; }

    public int getTotalParticlesCounted() { return totalParticlesCounted; }
    public void setTotalParticlesCounted(int totalParticlesCounted) { this.totalParticlesCounted = totalParticlesCounted; }

    public BigDecimal getConcentrationParticlesPerLiter() { return concentrationParticlesPerLiter; }
    public void setConcentrationParticlesPerLiter(BigDecimal concentrationParticlesPerLiter) { this.concentrationParticlesPerLiter = concentrationParticlesPerLiter; }

    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus status) { this.status = status; }

    public DataOrigin getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(DataOrigin dataOrigin) { this.dataOrigin = dataOrigin; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ParticleObservation> getObservations() { return observations; }
    public void setObservations(List<ParticleObservation> observations) { this.observations = observations; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

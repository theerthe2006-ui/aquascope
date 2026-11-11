package com.aquascope.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "particle_observations")
public class ParticleObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_analysis_id", nullable = false)
    private SampleAnalysis sampleAnalysis;

    @Column(name = "particle_count", nullable = false)
    private int particleCount = 1;

    @Column(name = "size_micrometers", nullable = false, precision = 10, scale = 2)
    private BigDecimal sizeMicrometers;

    @Column(name = "length_micrometers", precision = 10, scale = 2)
    private BigDecimal lengthMicrometers;

    @Column(name = "width_micrometers", precision = 10, scale = 2)
    private BigDecimal widthMicrometers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ParticleShape shape;

    @Column(length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "polymer_type", nullable = false, length = 100)
    private PolymerType polymerType = PolymerType.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "identification_method", nullable = false, length = 100)
    private IdentificationMethod identificationMethod;

    @Column(name = "confidence_level", nullable = false, length = 50)
    private String confidenceLevel = "LOW"; // LOW, MEDIUM, HIGH, DEFINITIVE

    @Column(name = "spectral_match_score", precision = 5, scale = 2)
    private BigDecimal spectralMatchScore;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public ParticleObservation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SampleAnalysis getSampleAnalysis() { return sampleAnalysis; }
    public void setSampleAnalysis(SampleAnalysis sampleAnalysis) { this.sampleAnalysis = sampleAnalysis; }

    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }

    public BigDecimal getSizeMicrometers() { return sizeMicrometers; }
    public void setSizeMicrometers(BigDecimal sizeMicrometers) { this.sizeMicrometers = sizeMicrometers; }

    public BigDecimal getLengthMicrometers() { return lengthMicrometers; }
    public void setLengthMicrometers(BigDecimal lengthMicrometers) { this.lengthMicrometers = lengthMicrometers; }

    public BigDecimal getWidthMicrometers() { return widthMicrometers; }
    public void setWidthMicrometers(BigDecimal widthMicrometers) { this.widthMicrometers = widthMicrometers; }

    public ParticleShape getShape() { return shape; }
    public void setShape(ParticleShape shape) { this.shape = shape; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public PolymerType getPolymerType() { return polymerType; }
    public void setPolymerType(PolymerType polymerType) { this.polymerType = polymerType; }

    public IdentificationMethod getIdentificationMethod() { return identificationMethod; }
    public void setIdentificationMethod(IdentificationMethod identificationMethod) { this.identificationMethod = identificationMethod; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public BigDecimal getSpectralMatchScore() { return spectralMatchScore; }
    public void setSpectralMatchScore(BigDecimal spectralMatchScore) { this.spectralMatchScore = spectralMatchScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
}

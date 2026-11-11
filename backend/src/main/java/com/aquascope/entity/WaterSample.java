package com.aquascope.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "water_samples")
public class WaterSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sample_identifier", nullable = false, unique = true, length = 100)
    private String sampleIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_site_id", nullable = false)
    private MonitoringSite monitoringSite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id")
    private User collectedByUser;

    @Column(name = "collection_date", nullable = false)
    private Instant collectionDate;

    @Column(name = "sample_volume", nullable = false, precision = 12, scale = 4)
    private BigDecimal sampleVolume;

    @Column(name = "volume_unit", nullable = false, length = 20)
    private String volumeUnit = "L";

    @Column(name = "sampling_depth_meters", precision = 6, scale = 2)
    private BigDecimal samplingDepthMeters;

    @Column(name = "water_temperature_celsius", precision = 5, scale = 2)
    private BigDecimal waterTemperatureCelsius;

    @Column(name = "ph_level", precision = 4, scale = 2)
    private BigDecimal phLevel;

    @Column(name = "turbidity_ntu", precision = 7, scale = 2)
    private BigDecimal turbidityNtu;

    @Column(name = "collection_source", nullable = false, length = 100)
    private String collectionSource;

    @Column(name = "weather_conditions", length = 150)
    private String weatherConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_origin", nullable = false, length = 50)
    private DataOrigin dataOrigin = DataOrigin.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SampleStatus status = SampleStatus.REGISTERED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public WaterSample() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSampleIdentifier() { return sampleIdentifier; }
    public void setSampleIdentifier(String sampleIdentifier) { this.sampleIdentifier = sampleIdentifier; }

    public MonitoringSite getMonitoringSite() { return monitoringSite; }
    public void setMonitoringSite(MonitoringSite monitoringSite) { this.monitoringSite = monitoringSite; }

    public User getCollectedByUser() { return collectedByUser; }
    public void setCollectedByUser(User collectedByUser) { this.collectedByUser = collectedByUser; }

    public Instant getCollectionDate() { return collectionDate; }
    public void setCollectionDate(Instant collectionDate) { this.collectionDate = collectionDate; }

    public BigDecimal getSampleVolume() { return sampleVolume; }
    public void setSampleVolume(BigDecimal sampleVolume) { this.sampleVolume = sampleVolume; }

    public String getVolumeUnit() { return volumeUnit; }
    public void setVolumeUnit(String volumeUnit) { this.volumeUnit = volumeUnit; }

    public BigDecimal getSamplingDepthMeters() { return samplingDepthMeters; }
    public void setSamplingDepthMeters(BigDecimal samplingDepthMeters) { this.samplingDepthMeters = samplingDepthMeters; }

    public BigDecimal getWaterTemperatureCelsius() { return waterTemperatureCelsius; }
    public void setWaterTemperatureCelsius(BigDecimal waterTemperatureCelsius) { this.waterTemperatureCelsius = waterTemperatureCelsius; }

    public BigDecimal getPhLevel() { return phLevel; }
    public void setPhLevel(BigDecimal phLevel) { this.phLevel = phLevel; }

    public BigDecimal getTurbidityNtu() { return turbidityNtu; }
    public void setTurbidityNtu(BigDecimal turbidityNtu) { this.turbidityNtu = turbidityNtu; }

    public String getCollectionSource() { return collectionSource; }
    public void setCollectionSource(String collectionSource) { this.collectionSource = collectionSource; }

    public String getWeatherConditions() { return weatherConditions; }
    public void setWeatherConditions(String weatherConditions) { this.weatherConditions = weatherConditions; }

    public DataOrigin getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(DataOrigin dataOrigin) { this.dataOrigin = dataOrigin; }

    public SampleStatus getStatus() { return status; }
    public void setStatus(SampleStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

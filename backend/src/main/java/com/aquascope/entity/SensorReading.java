package com.aquascope.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant readingTimestamp;

    @Column(name = "metric_type", nullable = false, length = 100)
    private String metricType; // TURBIDITY, FLUORESCENCE_ESTIMATE, etc.

    @Column(name = "metric_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal metricValue;

    @Column(nullable = false, length = 30)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_origin", nullable = false, length = 50)
    private DataOrigin dataOrigin = DataOrigin.DEVICE;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public SensorReading() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public Instant getReadingTimestamp() { return readingTimestamp; }
    public void setReadingTimestamp(Instant readingTimestamp) { this.readingTimestamp = readingTimestamp; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public DataOrigin getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(DataOrigin dataOrigin) { this.dataOrigin = dataOrigin; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Instant getCreatedAt() { return createdAt; }
}

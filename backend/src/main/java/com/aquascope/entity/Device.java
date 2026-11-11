package com.aquascope.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_identifier", nullable = false, unique = true, length = 100)
    private String deviceIdentifier;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "device_type", nullable = false, length = 100)
    private String deviceType; // OPTICAL_IN_SITU_SENSOR, AUTOSAMPLER, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_site_id")
    private MonitoringSite monitoringSite;

    @Column(length = 150)
    private String manufacturer;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "last_ping")
    private Instant lastPing;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Device() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceIdentifier() { return deviceIdentifier; }
    public void setDeviceIdentifier(String deviceIdentifier) { this.deviceIdentifier = deviceIdentifier; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public MonitoringSite getMonitoringSite() { return monitoringSite; }
    public void setMonitoringSite(MonitoringSite monitoringSite) { this.monitoringSite = monitoringSite; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    public Instant getLastPing() { return lastPing; }
    public void setLastPing(Instant lastPing) { this.lastPing = lastPing; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

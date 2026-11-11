package com.aquascope.service;

import com.aquascope.dto.DeviceDto;
import com.aquascope.dto.SensorReadingDto;
import com.aquascope.entity.DataOrigin;
import com.aquascope.entity.Device;
import com.aquascope.entity.DeviceStatus;
import com.aquascope.entity.MonitoringSite;
import com.aquascope.entity.SensorReading;
import com.aquascope.exception.DuplicateResourceException;
import com.aquascope.exception.ResourceNotFoundException;
import com.aquascope.repository.DeviceRepository;
import com.aquascope.repository.MonitoringSiteRepository;
import com.aquascope.repository.SensorReadingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final MonitoringSiteRepository monitoringSiteRepository;

    public DeviceService(
            DeviceRepository deviceRepository,
            SensorReadingRepository sensorReadingRepository,
            MonitoringSiteRepository monitoringSiteRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.monitoringSiteRepository = monitoringSiteRepository;
    }

    public List<DeviceDto> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::mapDeviceToDto)
                .collect(Collectors.toList());
    }

    public DeviceDto getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + id));
        return mapDeviceToDto(device);
    }

    @Transactional
    public DeviceDto registerDevice(DeviceDto dto) {
        if (deviceRepository.existsByDeviceIdentifier(dto.deviceIdentifier())) {
            throw new DuplicateResourceException("Device identifier already registered: " + dto.deviceIdentifier());
        }

        Device device = new Device();
        device.setDeviceIdentifier(dto.deviceIdentifier().toUpperCase());
        device.setName(dto.name());
        device.setDeviceType(dto.deviceType());
        device.setManufacturer(dto.manufacturer());
        device.setFirmwareVersion(dto.firmwareVersion());
        device.setStatus(dto.status() != null ? dto.status() : DeviceStatus.ACTIVE);
        device.setLastPing(Instant.now());

        if (dto.monitoringSiteId() != null) {
            MonitoringSite site = monitoringSiteRepository.findById(dto.monitoringSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found: " + dto.monitoringSiteId()));
            device.setMonitoringSite(site);
        }

        Device saved = deviceRepository.save(device);
        return mapDeviceToDto(saved);
    }

    @Transactional
    public DeviceDto updateDevice(Long id, DeviceDto dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + id));

        device.setName(dto.name());
        device.setDeviceType(dto.deviceType());
        device.setManufacturer(dto.manufacturer());
        device.setFirmwareVersion(dto.firmwareVersion());
        if (dto.status() != null) {
            device.setStatus(dto.status());
        }

        if (dto.monitoringSiteId() != null) {
            MonitoringSite site = monitoringSiteRepository.findById(dto.monitoringSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Monitoring site not found: " + dto.monitoringSiteId()));
            device.setMonitoringSite(site);
        } else {
            device.setMonitoringSite(null);
        }

        Device updated = deviceRepository.save(device);
        return mapDeviceToDto(updated);
    }

    @Transactional
    public SensorReadingDto recordSensorReading(SensorReadingDto dto) {
        Device device = deviceRepository.findById(dto.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + dto.deviceId()));

        // Update device last ping
        device.setLastPing(Instant.now());
        deviceRepository.save(device);

        SensorReading reading = new SensorReading();
        reading.setDevice(device);
        reading.setReadingTimestamp(dto.readingTimestamp() != null ? dto.readingTimestamp() : Instant.now());
        reading.setMetricType(dto.metricType());
        reading.setMetricValue(dto.metricValue());
        reading.setUnit(dto.unit());
        reading.setDataOrigin(dto.dataOrigin() != null ? dto.dataOrigin() : DataOrigin.DEVICE);
        reading.setRawPayload(dto.rawPayload());

        SensorReading saved = sensorReadingRepository.save(reading);
        return mapReadingToDto(saved);
    }

    public List<SensorReadingDto> getLatestReadingsByDevice(Long deviceId) {
        return sensorReadingRepository.findByDeviceIdOrderByReadingTimestampDesc(deviceId)
                .stream()
                .limit(50)
                .map(this::mapReadingToDto)
                .collect(Collectors.toList());
    }

    public Page<SensorReadingDto> getReadingsPage(Long deviceId, Pageable pageable) {
        return sensorReadingRepository.findByDeviceId(deviceId, pageable)
                .map(this::mapReadingToDto);
    }

    private DeviceDto mapDeviceToDto(Device d) {
        return new DeviceDto(
                d.getId(),
                d.getDeviceIdentifier(),
                d.getName(),
                d.getDeviceType(),
                d.getMonitoringSite() != null ? d.getMonitoringSite().getId() : null,
                d.getMonitoringSite() != null ? d.getMonitoringSite().getCode() : null,
                d.getManufacturer(),
                d.getFirmwareVersion(),
                d.getStatus(),
                d.getLastPing(),
                d.getCreatedAt()
        );
    }

    private SensorReadingDto mapReadingToDto(SensorReading r) {
        return new SensorReadingDto(
                r.getId(),
                r.getDevice().getId(),
                r.getDevice().getDeviceIdentifier(),
                r.getReadingTimestamp(),
                r.getMetricType(),
                r.getMetricValue(),
                r.getUnit(),
                r.getDataOrigin(),
                r.getRawPayload(),
                r.getCreatedAt()
        );
    }
}

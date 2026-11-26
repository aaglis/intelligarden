package com.smartgarden.backend.device;

import com.smartgarden.backend.common.ResourceAlreadyExistsException;
import com.smartgarden.backend.device.dto.CreateDeviceRequest;
import com.smartgarden.backend.device.dto.DeviceResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeviceService(DeviceRepository deviceRepository, ApplicationEventPublisher eventPublisher) {
        this.deviceRepository = deviceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Atualiza o estado online/offline informado pelo dispositivo no tópico .../status.
     * Só publica o evento quando o estado realmente muda (a mensagem retida chega de novo a cada reconexão do backend).
     */
    @Transactional
    public DeviceResponse updateStatus(String deviceCode, boolean online, String ipAddress, String firmwareVersion) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseGet(() -> {
                    Device created = new Device();
                    created.setDeviceCode(deviceCode);
                    created.setName("ESP32 " + deviceCode);
                    created.setActive(true);
                    return created;
                });

        boolean changed = device.getId() == null || device.isOnline() != online;
        OffsetDateTime now = OffsetDateTime.now();
        if (changed) {
            device.setOnline(online);
            device.setStatusChangedAt(now);
        }
        if (online) {
            device.setLastSeenAt(now);
            if (ipAddress != null && !ipAddress.isBlank()) {
                device.setIpAddress(ipAddress);
            }
            if (firmwareVersion != null && !firmwareVersion.isBlank()) {
                device.setFirmwareVersion(firmwareVersion);
            }
        }

        DeviceResponse response = DeviceResponse.fromEntity(deviceRepository.save(device));
        if (changed) {
            eventPublisher.publishEvent(new DeviceStatusChangedEvent(response));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String deviceCode) {
        return deviceRepository.findByDeviceCode(deviceCode)
                .map(DeviceResponse::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Dispositivo nao encontrado: " + deviceCode));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices() {
        return deviceRepository.findAllByOrderByNameAsc().stream()
                .map(DeviceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeviceResponse createDevice(CreateDeviceRequest request) {
        if (deviceRepository.existsByDeviceCode(request.deviceCode())) {
            throw new ResourceAlreadyExistsException("Ja existe um dispositivo com o codigo informado.");
        }

        Device device = new Device();
        device.setDeviceCode(request.deviceCode());
        device.setName(request.name());
        device.setLocation(request.location());
        device.setActive(true);

        return DeviceResponse.fromEntity(deviceRepository.save(device));
    }

    @Transactional
    public Device findOrCreateByCode(String deviceCode) {
        return deviceRepository.findByDeviceCode(deviceCode)
                .map(existing -> {
                    existing.setLastSeenAt(OffsetDateTime.now());
                    return existing;
                })
                .orElseGet(() -> {
                    Device device = new Device();
                    device.setDeviceCode(deviceCode);
                    device.setName("ESP32 " + deviceCode);
                    device.setActive(true);
                    device.setLastSeenAt(OffsetDateTime.now());
                    return deviceRepository.save(device);
                });
    }

    @Transactional(readOnly = true)
    public long countDevices() {
        return deviceRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveDevices() {
        return deviceRepository.countByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Device> listDeviceEntities() {
        return deviceRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Device> findByCode(String deviceCode) {
        return deviceRepository.findByDeviceCode(deviceCode);
    }
}

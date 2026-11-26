package com.smartgarden.backend.device;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceStatusTest {

    private DeviceRepository repository;
    private ApplicationEventPublisher publisher;
    private DeviceService service;
    private Device device;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(DeviceRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new DeviceService(repository, publisher);

        device = new Device();
        device.setDeviceCode("esp32-jardim");
        device.setName("ESP32");
        var id = Device.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(device, 1L);

        when(repository.findByDeviceCode("esp32-jardim")).thenReturn(Optional.of(device));
        when(repository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldPublishEventWhenDeviceComesOnline() {
        var response = service.updateStatus("esp32-jardim", true, "10.50.22.73", "1.1.0");

        assertThat(response.online()).isTrue();
        assertThat(response.ipAddress()).isEqualTo("10.50.22.73");
        assertThat(response.firmwareVersion()).isEqualTo("1.1.0");
        assertThat(response.statusChangedAt()).isNotNull();
        verify(publisher).publishEvent(any(DeviceStatusChangedEvent.class));
    }

    @Test
    void shouldNotPublishAgainWhenRetainedStatusRepeats() {
        device.setOnline(true);

        service.updateStatus("esp32-jardim", true, "10.50.22.73", "1.1.0");

        verify(publisher, never()).publishEvent(any(DeviceStatusChangedEvent.class));
    }

    @Test
    void shouldKeepLastIpWhenGoingOffline() {
        device.setOnline(true);
        device.setIpAddress("10.50.22.73");

        var response = service.updateStatus("esp32-jardim", false, null, null);

        assertThat(response.online()).isFalse();
        assertThat(response.ipAddress()).isEqualTo("10.50.22.73");
        verify(publisher).publishEvent(any(DeviceStatusChangedEvent.class));
    }
}

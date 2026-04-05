package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.DeviceDto;
import com.smarthome.smart_home_ai.exception.EntityNotFoundException;
import com.smarthome.smart_home_ai.mapper.DeviceMapper;
import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    @Transactional(readOnly = true)
    public List<DeviceDto> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toDto)
                .toList();
    }

    @Transactional
    public DeviceDto createDevice(DeviceDto deviceDto) {
        Device device = deviceMapper.toEntity(deviceDto);
        Device deviceSaved = deviceRepository.save(device);
        return deviceMapper.toDto(deviceSaved);
    }

    @Transactional
    public DeviceDto toggleDeviceStatus(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device with id " + id + " not found"));

        device.setStatus(device.getStatus() == DeviceStatus.ON ? DeviceStatus.OFF : DeviceStatus.ON);
        return deviceMapper.toDto(deviceRepository.save(device));
    }

    @Transactional
    public DeviceDto updateDeviceValue(Long id, Integer newValue) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device with id " + id + " not found"));

        device.setTargetValue(newValue);

        // Умная фича: если мы просим сделать громкость 50 или температуру 25,
        // логично заодно убедиться, что устройство Включено (ON)
        if (device.getStatus() == DeviceStatus.OFF) {
            device.setStatus(DeviceStatus.ON);
        }

        Device savedDevice = deviceRepository.save(device);
        return deviceMapper.toDto(savedDevice);
    }

    // Метод для внутрішнього використання сервісами (не віддає DTO)
    @Transactional(readOnly = true)
    public List<Device> getAllEntities() {
        return deviceRepository.findAll();
    }

    // Метод для масового збереження (використовується в сценаріях)
    @Transactional
    public void saveAllEntities(List<Device> devices) {
        deviceRepository.saveAll(devices);
    }
}

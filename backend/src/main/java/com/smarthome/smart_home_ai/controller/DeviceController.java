package com.smarthome.smart_home_ai.controller;

import com.smarthome.smart_home_ai.dto.DeviceDto;
import com.smarthome.smart_home_ai.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceDto> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @PostMapping
    public DeviceDto createDevice(@Valid @RequestBody DeviceDto deviceDto) {
        return deviceService.createDevice(deviceDto);
    }

    @PutMapping("/{id}/toggle")
    public DeviceDto toggleDevice(@PathVariable Long id) {
        return deviceService.toggleDeviceStatus(id);
    }

    @PutMapping("/{id}/value")
    public DeviceDto updateDeviceValue(@PathVariable Long id, @RequestParam Integer value) {
        return deviceService.updateDeviceValue(id, value);
    }
}

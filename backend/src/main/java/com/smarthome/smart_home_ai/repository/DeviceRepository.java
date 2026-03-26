package com.smarthome.smart_home_ai.repository;

import com.smarthome.smart_home_ai.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}

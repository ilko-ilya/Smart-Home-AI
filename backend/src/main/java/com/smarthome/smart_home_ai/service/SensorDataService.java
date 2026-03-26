package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.SensorDataDto;
import com.smarthome.smart_home_ai.exception.EntityNotFoundException;
import com.smarthome.smart_home_ai.mapper.SensorDataMapper;
import com.smarthome.smart_home_ai.model.SensorData;
import com.smarthome.smart_home_ai.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;
    private final SensorDataMapper sensorDataMapper;

    @Transactional
    public SensorDataDto saveReading(SensorDataDto dto) {
        SensorData entity = sensorDataMapper.toEntity(dto);
        SensorData saved = sensorDataRepository.save(entity);
        return sensorDataMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public SensorDataDto getLatestReading() {
        return sensorDataRepository.findTopByOrderByTimestampDesc()
                .map(sensorDataMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("No sensor data found"));
    }
}

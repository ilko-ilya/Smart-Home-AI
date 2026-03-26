package com.smarthome.smart_home_ai.repository;

import com.smarthome.smart_home_ai.model.SensorData;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SensorDataRepository extends CrudRepository<SensorData, Long> {

    // Получить самые свежие показания датчиков
    Optional<SensorData> findTopByOrderByTimestampDesc();

}

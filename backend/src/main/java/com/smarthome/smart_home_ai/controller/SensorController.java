package com.smarthome.smart_home_ai.controller;

import com.smarthome.smart_home_ai.dto.SensorDataDto;
import com.smarthome.smart_home_ai.service.SensorDataService;
import com.smarthome.smart_home_ai.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorDataService sensorDataService;
    private final WeatherService weatherService;

    @PostMapping
    public SensorDataDto addReading(@RequestBody SensorDataDto dto) {
        return sensorDataService.saveReading(dto);
    }

    @GetMapping("/latest")
    public SensorDataDto getLatestReading() {
        return sensorDataService.getLatestReading();
    }

    @GetMapping("/weather")
    public Double getOutsideWeather() {
        return weatherService.getCurrentOutsideTemperature();
    }

}

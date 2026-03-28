package com.smarthome.smart_home_ai.controller;

import com.smarthome.smart_home_ai.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*") // Разрешаем фронтенду обращаться к этому API
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public Double getCurrentWeather() {
        return weatherService.getCurrentOutsideTemperature();
    }
}

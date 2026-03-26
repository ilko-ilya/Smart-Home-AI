package com.smarthome.smart_home_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${weather.url}")
    private String weatherUrl;

    private final RestClient restClient;

    public Double getCurrentOutsideTemperature() {
        try {
            OpenMeteoResponse response = restClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response != null && response.current_weather() != null) {
                Double temp = response.current_weather().temperature();
                log.info("Успешно получена погода через API: {} градусов", temp);
                return temp;
            } else {
                log.warn("API погоды вернул пустой результат");
                return null;
            }

        } catch (Exception e) {
            log.error("Ошибка при получении погоды по API: {}", e.getMessage(), e);
            return null;
        }
    }

    // DTO для маппинга JSON
    record OpenMeteoResponse(CurrentWeather current_weather) {}
    record CurrentWeather(Double temperature) {}

}

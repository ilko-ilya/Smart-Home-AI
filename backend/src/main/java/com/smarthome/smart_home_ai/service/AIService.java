package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.AiResponseDto;
import com.smarthome.smart_home_ai.dto.DeviceDto;
import com.smarthome.smart_home_ai.dto.SensorDataDto;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;
    private final WeatherService weatherService;
    private final RestClient restClient;

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.url}")
    private String apiUrl;

    public AiResponseDto getRecommendation() {
        // 1. СОБИРАЕМ ДАННЫЕ СО ВСЕГО ДОМА
        Double outsideTemp = weatherService.getCurrentOutsideTemperature();
        SensorDataDto insideData = getLatestSensorDataSafe();
        List<DeviceDto> devices = deviceService.getAllDevices();

        // Фильтруем только включенные устройства
        String activeDevices = devices.stream()
                .filter(d -> d.status() == DeviceStatus.ON)
                .map(DeviceDto::name)
                .collect(Collectors.joining(", "));

        if (activeDevices.isEmpty()) activeDevices = "Ничего не включено";

        // 2. ФОРМИРУЕМ PROMPT (Текст запроса)
        String prompt = String.format(
                "Температура на улице: %s °C. " +
                        "Температура дома: %s °C (влажность %s%%). " +
                        "Включенные устройства: %s. " +
                        "Опираясь на эти данные, дай короткий, полезный и креативный совет (1-2 предложения) по комфорту или энергосбережению. Обращайся к пользователю 'сэр'.",
                outsideTemp,
                insideData != null ? insideData.temperature() : "Неизвестно",
                insideData != null ? insideData.humidity() : "Неизвестно",
                activeDevices
        );

        log.info("Запрашиваем совет у ИИ: {}", prompt);

        // 3. ВЫЗЫВАЕМ БЕСПЛАТНУЮ НЕЙРОСЕТЬ (Llama 3.1)
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            // Даем нейросети роль!
                            Map.of("role", "system", "content", "Ты AI-ассистент умного дома (Джарвис). Отвечай коротко, по делу, с долей вежливого сарказма или заботы."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7 // Здесь 0.7 нормально, так как нам нужна креативность для генерации советов
            );

            JsonNode rootResponse = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            assert rootResponse != null;
            String advice = rootResponse.path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asString();

            return new AiResponseDto(advice);

        } catch (Exception e) {
            log.error("Ошибка при запросе к Groq API: {}", e.getMessage(), e);
            return new AiResponseDto("Простите, сэр. Мои модули анализа среды временно недоступны.");
        }
    }

    private SensorDataDto getLatestSensorDataSafe() {
        try {
            return sensorDataService.getLatestReading();
        } catch (Exception e) {
            return null;
        }
    }
}
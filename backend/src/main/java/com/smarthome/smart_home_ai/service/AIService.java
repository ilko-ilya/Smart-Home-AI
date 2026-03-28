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
        // 1. ЗБИРАЄМО ДАНІ З УСЬОГО БУДИНКУ
        Double outsideTemp = weatherService.getCurrentOutsideTemperature();
        SensorDataDto insideData = getLatestSensorDataSafe();
        List<DeviceDto> devices = deviceService.getAllDevices();

        // Фільтруємо тільки увімкнені пристрої
        String activeDevices = devices.stream()
                .filter(d -> d.status() == DeviceStatus.ON)
                .map(DeviceDto::name)
                .collect(Collectors.joining(", "));

        if (activeDevices.isEmpty()) activeDevices = "Нічого не увімкнено";

        // 2. ФОРМУЄМО PROMPT (Текст запиту українською)
        String prompt = String.format(
                "Температура надворі: %s °C. " +
                        "Температура вдома: %s °C (вологість %s%%). " +
                        "Увімкнені пристрої: %s. " +
                        "Спираючись на ці дані, дай коротку, корисну та креативну пораду (1-2 речення) щодо комфорту або енергозбереження. Обов'язково звертайся до користувача 'сер'.",
                outsideTemp,
                insideData != null ? insideData.temperature() : "Невідомо",
                insideData != null ? insideData.humidity() : "Невідомо",
                activeDevices
        );

        log.info("Запитуємо пораду в ШІ: {}", prompt);

        // 3. ВИКЛИКАЄМО НЕЙРОМЕРЕЖУ (Llama 3.1)
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of("role", "system", "content", "Ти елітний AI-дворецький (Джарвіс). Твоя мета - дати ОДНУ коротку та абсолютно логічну пораду (максимум 2 речення). Категорично заборонено повторювати слова або писати безглуздий текст. Завжди звертайся до користувача 'сер'."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3 // 🔥 Знизили температуру, щоб прибрати галюцинації
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

            // Додаткова перевірка: якщо ІА все ж таки видав бред, обрізаємо його
            if (advice.length() > 200) {
                advice = advice.substring(0, 200) + "...";
            }

            return new AiResponseDto(advice);

        } catch (Exception e) {
            log.error("Помилка під час запиту до Groq API: {}", e.getMessage(), e);
            return new AiResponseDto("Вибачте, сер. Мої модулі аналізу тимчасово перевантажені.");
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

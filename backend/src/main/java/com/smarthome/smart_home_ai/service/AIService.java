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

    // 🔥 Вирішення Пункту 3.1: Використовуємо наш єдиний клієнт замість дублювання RestClient
    private final AiClient aiClient;

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

        // 2. ФОРМУЄМО PROMPT
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

        // 3. ВИКЛИКАЄМО НЕЙРОМЕРЕЖУ ЧЕРЕЗ AiClient
        try {
            String systemPrompt = "Ти елітний AI-дворецький (Джарвіс). Твоя мета - дати ОДНУ коротку та абсолютно логічну пораду (максимум 2 речення). Категорично заборонено повторювати слова або писати безглуздий текст. Завжди звертайся до користувача 'сер'.";

            // Делегуємо запит у спеціальний клієнт
            String advice = aiClient.getAdvice(systemPrompt, prompt);

            // 🔥 Вирішення Пункту 1.4: Нормальна перевірка на null замість assert
            if (advice == null || advice.isBlank()) {
                log.error("AI повернув порожню відповідь");
                return new AiResponseDto("Вибачте, сер. Мої модулі аналізу тимчасово перевантажені.");
            }

            if (advice.length() > 200) {
                advice = advice.substring(0, 200) + "...";
            }

            return new AiResponseDto(advice);

        } catch (Exception e) {
            log.error("Помилка під час отримання рекомендації: {}", e.getMessage(), e);
            return new AiResponseDto("Вибачте, сер. Виникла помилка в системі аналітики.");
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

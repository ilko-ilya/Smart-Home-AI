package com.smarthome.smart_home_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiClient {

    private final RestClient restClient;

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.url}")
    private String apiUrl;

    public AiClient() {
        this.restClient = RestClient.create();
    }

    public String callAi(String text, String devicesJson) {
        Map<String, Object> requestBody = buildPrompt(text, devicesJson);

        try {
            JsonNode response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("choices")) {
                String rawContent = response.path("choices").get(0).path("message").path("content").asText();
                log.info("RAW ВІДПОВІДЬ ШІ: \n{}", rawContent); // Логуємо сиру відповідь для дебагу
                return rawContent;
            }
            throw new RuntimeException("Порожня відповідь від API");

        } catch (Exception e) {
            log.error("Помилка під час виклику ШІ-моделі: {}", e.getMessage());
            throw new RuntimeException("Помилка мережі ШІ", e);
        }
    }

    private Map<String, Object> buildPrompt(String text, String devicesJson) {
        String systemPrompt = "Ти - REST API сервер розумного будинку. Твоя єдина задача - генерувати JSON. " +
                "КАТЕГОРИЧНО ЗАБОРОНЕНО писати програмний код, пояснення або текст.\n" +
                "Доступні пристрої: " + devicesJson + "\n" +
                "Команда користувача: '" + text + "'\n" +
                "ПРАВИЛА:\n" +
                "1. Поверни масив ТІЛЬКИ з тими пристроями, які потрібно змінити.\n" +
                "2. 'targetStatus' - строго 'ON' або 'OFF'.\n" +
                "3. 'targetValue' - це число або null. УВАГА: використовуй число ТІЛЬКИ для пристроїв, де це логічно. Для звичайних приладів (кавоварка, чайник, телевізор, пилосос) targetValue ЗАВЖДИ має бути строго null!\n\n" +
                "🔥 ЖОРСТКІ СЦЕНАРІЇ (ВИКОНУЙ БЕЗЗАПЕРЕЧНО):\n" +
                "- Якщо користувач каже 'Я вдома', 'Я повернувся' тощо: SECURITY_SENSOR = OFF, VACUUM = OFF, Основне світло = ON, STEREO = ON (з targetValue: 20).\n" +
                "- Якщо користувач каже 'Я йду', 'Йду з дому', 'Виходжу' тощо: SECURITY_SENSOR = ON, VACUUM = ON, а АБСОЛЮТНО ВСІ ІНШІ увімкнені пристрої (світло, кавоварка, телевізор, стерео тощо) = OFF.\n\n" +
                "ПРИКЛАД ВІДПОВІДІ:\n" +
                "[\n" +
                "  {\"deviceId\": 3, \"targetStatus\": \"ON\", \"targetValue\": null}\n" +
                "]";

        return Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text) // Добавили передачу самого текста отдельным сообщением для надежности
                ),
                "temperature", 0.0 // Температура 0 робить відповіді максимально суворими і без фантазій
        );
    }
}

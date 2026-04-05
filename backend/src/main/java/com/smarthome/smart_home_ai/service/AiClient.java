package com.smarthome.smart_home_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiToolsDefinition toolsDefinition;

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.url}")
    private String apiUrl;

    // 🔥 ГЛАВНЫЙ МЕТОД LLM (с флагом allowTools)
    public JsonNode callAi(List<Map<String, Object>> messages,
                           String devicesJson,
                           boolean allowTools) {
        try {
            String systemPrompt = """
                    Ти - Джарвіс, елітний штучний інтелект розумного будинку.
                   \s
                    КРИТИЧНЕ ПРАВИЛО (МОВНЕ):
                    ТИ ЗАВЖДИ ВІДПОВІДАЄШ ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ.
                   \s
                    АБСОЛЮТНО НЕВАЖЛИВО:
                    - якою мовою до тебе звернувся користувач (навіть якщо російською)
                    - якою мовою ти отримав дані з інтернету
                   \s
                    ЯКЩО ТВОЯ ВІДПОВІДЬ НЕ УКРАЇНСЬКОЮ — ЦЕ ФАТАЛЬНА ПОМИЛКА.
                    ЗАВЖДИ перекладай свої думки та результати пошуку на ідеальну українську мову.
                    Завжди звертайся до користувача 'сер'.
                   \s
                    ПРАВИЛО ІНСТРУМЕНТІВ:
                    Якщо ти викликав search_web (пошук), ти ОБОВ'ЯЗКОВО маєш видати коротку текстову відповідь.
                    Якщо питали про погоду/температуру — обов'язково назви точні цифри градусів! НЕ МОВЧИ!
                   \s
                    Поточний стан пристроїв:\s""" + devicesJson;

            List<Map<String, Object>> finalMessages = new ArrayList<>();
            finalMessages.add(Map.of("role", "system", "content", systemPrompt));
            finalMessages.addAll(messages);

            Map<String, Object> requestBody = new HashMap<>();
            // 🔥 ВЗРОСЛАЯ МОДЕЛЬ (не ленится) и низкая температура (меньше галлюцинаций)
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("temperature", 0.1);
            requestBody.put("messages", finalMessages);

            // 🔥 tools добавляем ТОЛЬКО если нужно (при первом круге)
            if (allowTools) {
                JsonNode toolsNode = objectMapper.readTree(toolsDefinition.getToolsJson());
                requestBody.put("tools", toolsNode);
                requestBody.put("tool_choice", "auto");
            }

            return restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

        } catch (Exception e) {
            log.error("Помилка ШІ: ", e);
            throw new RuntimeException("AI error", e);
        }
    }

    // 🎤 Whisper (стрим)
    public String transcribeAudioBytes(byte[] audioBytes) {
        log.info("Відправляю WAV у Whisper ({} байт)", audioBytes.length);
        try {
            MultiValueMap<String, Object> body = buildBody(audioBytes);

            JsonNode response = restClient.post()
                    .uri("https://api.groq.com/openai/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("text")) {
                String text = response.get("text").asString().trim();
                log.info("🧠 RAW Whisper: {}", text);
                return text;
            }

            return "";

        } catch (Exception e) {
            log.error("Помилка Whisper:", e);
            return "";
        }
    }

    // Метод для обробки MultipartFile (REST API)
    public String transcribeAudio(org.springframework.web.multipart.MultipartFile audioFile) {
        log.info("Відправляю аудіо у Whisper (REST API)...");
        try {
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", audioFile.getResource());
            body.add("model", "whisper-large-v3");
            body.add("prompt", "Джарвис, Джарвіс, Бровары, Бровари, увімкни, включи, світло, телевізор.");
            body.add("temperature", "0");
            body.add("response_format", "json");

            JsonNode response = restClient.post()
                    .uri("https://api.groq.com/openai/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("text")) {
                return response.get("text").asString().trim();
            }
            return "";
        } catch (Exception e) {
            log.error("Помилка Whisper API (REST): ", e);
            return "";
        }
    }

    private MultiValueMap<String, Object> buildBody(byte[] audioBytes) {
        ByteArrayResource resource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("model", "whisper-large-v3");

        // 🔥 Правильний магніт-словник замість інструкцій
        body.add("prompt", "Джарвис, Джарвіс, Бровары, Бровари, увімкни, включи, світло, телевізор.");

        // 🔥 Нульова температура рятує від галюцинацій на тиші
        body.add("temperature", "0");
        body.add("response_format", "json");

        return body;
    }

    // Метод для генерації звичайних порад (без виклику інструментів)
    public String getAdvice(String systemPrompt, String userPrompt) {
        try {
            java.util.Map<String, Object> requestBody = java.util.Map.of(
                    "model", "llama-3.3-70b-versatile", // 🔥 Оновив модель на актуальну
                    "messages", java.util.List.of(
                            java.util.Map.of("role", "system", "content", systemPrompt),
                            java.util.Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.3
            );

            JsonNode response = restClient.post()
                    .uri("""
                            https://api.groq.com/openai/v1/chat/completions""")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("choices")) {
                return response.path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asString();
            }
            return null;
        } catch (Exception e) {
            log.error("Помилка AiClient (getAdvice): ", e);
            return null;
        }
    }
}

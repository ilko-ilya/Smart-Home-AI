package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.AiActionDto;
import com.smarthome.smart_home_ai.dto.DeviceDto;
import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JarvisService {

    private final AiClient aiClient;
    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;
    private final SmartSearchTool searchTool;
    private final ScenarioService scenarioService;

    public String processVoiceCommand(String text) {
        Optional<String> scenarioResponse = scenarioService.handleScenario(text.toLowerCase());
        return scenarioResponse.orElseGet(() -> handleAgenticWorkflow(text));
    }

    private String handleAgenticWorkflow(String text) {
        try {
            // 🔥 Пункт 2.4: Використовуємо DTO замість сирих Entity для контексту LLM
            List<DeviceDto> devices = deviceService.getAllDevices();
            String devicesJson = objectMapper.writeValueAsString(devices);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", text));

            // 1. Перший виклик (З tools)
            JsonNode response = aiClient.callAi(messages, devicesJson, true);
            JsonNode message = response.path("choices").get(0).path("message");

            // якщо без tools, просто повертаємо текст
            if (!message.has("tool_calls") || message.path("tool_calls").isEmpty()) {
                return message.path("content").asString();
            }

            // 2. Правильно зберігаємо історію з content
            Map<String, Object> assistant = new HashMap<>();
            assistant.put("role", "assistant");
            assistant.put("tool_calls", message.path("tool_calls"));
            if (message.has("content") && !message.path("content").isNull()) {
                assistant.put("content", message.path("content").asString());
            }
            messages.add(assistant);

            boolean needSecondCall = false;
            StringBuilder fastResponse = new StringBuilder();

            // 3. Виконання tools
            for (JsonNode toolCall : message.path("tool_calls")) {

                String id = toolCall.path("id").asString();
                String name = toolCall.path("function").path("name").asString();
                String args = toolCall.path("function").path("arguments").asString();

                String result;

                if ("control_devices".equals(name)) {
                    result = executeDeviceControl(args);
                    fastResponse.append(result).append(" ");
                } else if ("search_web".equals(name)) {
                    result = executeWebSearch(args);

                    // Очищаємо сміття з пошуку
                    result = cleanSearchResult(result);
                    result += """
                            
                            
                            [СИСТЕМНЕ ПРАВИЛО: На основі цих даних сформуй коротку відповідь для користувача \
                            українською мовою. ОБОВ'ЯЗКОВО назви точні цифри градусів! НЕ МОВЧИ!]""";

                    needSecondCall = true;
                } else {
                    result = "unknown tool";
                }

                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", id,
                        "name", name,
                        "content", result
                ));
            }

            // швидка відповідь без другого AI
            if (!needSecondCall && !fastResponse.isEmpty()) {
                return fastResponse.toString().trim();
            }

            // 4. Другий виклик (БЕЗ tools)
            JsonNode finalResponse = aiClient.callAi(messages, "{}", false);

            JsonNode contentNode = finalResponse.path("choices")
                    .get(0)
                    .path("message")
                    .path("content");

            String finalText = contentNode.isNull() ? "" : contentNode.asString().trim();

            // 5. Захист від порожнечі
            if (finalText.isEmpty()) {
                log.error("❌ Порожня відповідь від AI після search");
                return "Не вдалося отримати відповідь, сер.";
            }

            if (finalText.toLowerCase().contains("лог")) {
                log.warn("❌ AI вернув сміття: {}", finalText);
                return "Отримав дані, сер. Виводжу їх на екран.";
            }

            return finalText;

        } catch (Exception e) {
            log.error("Agent error:", e);
            return "Помилка системи, сер.";
        }
    }

    private String cleanSearchResult(String text) {
        if (text == null) return "";
        return text
                .replaceAll("http\\S+", "") // Видаляємо посилання
                .replaceAll("(?i)log.*", "") // Видаляємо логи
                .replaceAll("\\n{2,}", "\n") // Прибираємо порожні рядки
                .trim();
    }

    private String executeDeviceControl(String argsJson) {
        try {
            JsonNode argsNode = objectMapper.readTree(argsJson);
            JsonNode actionsNode = argsNode.path("actions");

            List<AiActionDto> actions = objectMapper.convertValue(actionsNode, new TypeReference<>() {});

            // 🔥 Отримуємо сутності через безпечний шар сервісу
            List<Device> allDevices = deviceService.getAllEntities();
            List<Device> devicesToUpdate = new ArrayList<>();
            List<String> executed = new ArrayList<>();

            for (AiActionDto action : actions) {
                allDevices.stream()
                        .filter(d -> d.getId().equals(action.deviceId()))
                        .findFirst()
                        .ifPresent(device -> {
                            device.setStatus(DeviceStatus.valueOf(action.targetStatus()));
                            devicesToUpdate.add(device);

                            String status = "ON".equals(action.targetStatus()) ? "увімкнено" : "вимкнено";
                            executed.add(device.getName() + " " + status);
                        });
            }

            // 🔥 Зберігаємо через безпечний шар сервісу
            deviceService.saveAllEntities(devicesToUpdate);

            if (executed.isEmpty()) {
                return "Нічого не змінено.";
            }

            return "Зроблено, сер. " + String.join(", ", executed) + ".";

        } catch (Exception e) {
            log.error("Помилка виконання control_devices: ", e);
            return "Помилка керування.";
        }
    }

    // Метод для обробки аудіо-файлів (HTTP REST)
    public String processAudioCommand(org.springframework.web.multipart.MultipartFile audioFile) {
        String text = aiClient.transcribeAudio(audioFile);

        if (text == null || text.trim().isEmpty()) {
            return "Вибачте, сер. Я не розчув вашу команду.";
        }

        String lowerText = text.toLowerCase();
        if (!lowerText.contains("джарвіс") && !lowerText.contains("джарвис") && !lowerText.contains("jarvis")) {
            log.info("Проігноровано Whisper: немає слова-тригера. Почуто: {}", text);
            return "";
        }

        String command = lowerText
                .replaceAll("(?i)(джарвіс|джарвис|jarvis)[.,!?-]*", "")
                .trim();

        if (command.isEmpty()) {
            return "Я вас слухаю, сер. Яку команду виконати?";
        }

        log.info("Передаємо в Agentic Workflow очищену команду: {}", command);
        return processVoiceCommand(command);
    }

    private String executeWebSearch(String argsJson) {
        try {
            JsonNode argsNode = objectMapper.readTree(argsJson);
            String query = argsNode.path("query").asString();

            log.info("🔍 search: {}", query);

            return searchTool.executeSearch(query);

        } catch (Exception e) {
            log.error("Помилка пошуку: ", e);
            return "Помилка пошуку.";
        }
    }
}

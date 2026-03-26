package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.AiActionDto;
import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class JarvisService {
    private final DeviceRepository deviceRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Value("${ai.groq.url}")
    private String apiUrl;

    public JarvisService(DeviceRepository deviceRepository, ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public String processVoiceCommand(String text) {
        String lowerText = text.toLowerCase();

        // ==========================================
        // 1. ЖЕСТКИЙ ПЕРЕХВАТ СЦЕНАРИЕВ (БЕЗ ИИ)
        // ==========================================

        // Сценарий А: Я ухожу
        if (lowerText.contains("ухожу") || lowerText.contains("ушел") || lowerText.contains("охрану")) {
            List<Device> allDevices = deviceRepository.findAll();
            int updatedCount = 0;

            for (Device device : allDevices) {
                String type = device.getType().name();
                if (type.equals("SECURITY_SENSOR") || type.equals("MOTION_SENSOR") || type.equals("DOOR_SENSOR") || type.equals("VACUUM")) {
                    device.setStatus(DeviceStatus.ON);
                } else {
                    device.setStatus(DeviceStatus.OFF);
                }
                deviceRepository.save(device);
                updatedCount++;
            }
            return "Протокол защиты активирован, сэр. Дом поставлен на охрану, приборы обесточены.";
        }

        // Сценарий Б: Я пришел / Я дома
        if (lowerText.contains("пришел") || lowerText.contains("дома") || lowerText.contains("вернулся")) {
            List<Device> allDevices = deviceRepository.findAll();
            int updatedCount = 0;

            for (Device device : allDevices) {
                String type = device.getType().name();
                String name = device.getName().toLowerCase();

                // 1. Отключаем охрану и пылесос
                if (type.equals("SECURITY_SENSOR") || type.equals("MOTION_SENSOR") || type.equals("DOOR_SENSOR") || type.equals("VACUUM")) {
                    device.setStatus(DeviceStatus.OFF);
                }
                // 2. Включаем легкую музыку (Стерео на 20%)
                else if (type.equals("STEREO")) {
                    device.setStatus(DeviceStatus.ON);
                    device.setTargetValue(20);
                }
                // 3. Включаем Основной свет
                else if (type.equals("LIGHT") && name.contains("основной свет")) {
                    device.setStatus(DeviceStatus.ON);
                    device.setTargetValue(100);
                }

                deviceRepository.save(device);
                updatedCount++;
            }
            return "С возвращением, сэр! Охрана снята. Включаю свет и легкую музыку.";
        }

        // ==========================================
        // 2. ОБЫЧНЫЕ КОМАНДЫ (ОТПРАВЛЯЕМ К ИИ)
        // ==========================================
        List<Device> devices = deviceRepository.findAll();

        try {
            String devicesJson = objectMapper.writeValueAsString(devices);
            Map<String, Object> requestBody = getStringObjectMap(text, devicesJson);

            JsonNode rootResponse = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            assert rootResponse != null;
            String aiContent = rootResponse.path("choices").get(0).path("message").path("content").asText();

            log.info("ОРИГИНАЛЬНЫЙ ОТВЕТ ИИ: \n{}", aiContent);

            // ЛЕЧИМ ОШИБКУ PYTHON ('None' -> 'null')
            aiContent = aiContent.replace("None", "null").replace("NONE", "null");

            // БРОНЕБОЙНЫЙ ЭКСТРАКТОР JSON (Отрезаем лишний текст)
            int startArray = aiContent.indexOf('[');
            int startObj = aiContent.indexOf('{');
            int endArray = aiContent.lastIndexOf(']');
            int endObj = aiContent.lastIndexOf('}');

            int startIndex = -1;
            if (startArray != -1 && startObj != -1) startIndex = Math.min(startArray, startObj);
            else if (startArray != -1) startIndex = startArray;
            else if (startObj != -1) startIndex = startObj;

            int endIndex = -1;
            if (endArray != -1 && endObj != -1) endIndex = Math.max(endArray, endObj);
            else if (endArray != -1) endIndex = endArray;
            else if (endObj != -1) endIndex = endObj;

            if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                aiContent = aiContent.substring(startIndex, endIndex + 1);
            } else {
                return "Сэр, ИИ вернул ответ без структуры данных. Попробуйте еще раз.";
            }

            // ГИБКИЙ ПАРСИНГ
            JsonNode rootNode = objectMapper.readTree(aiContent);
            List<AiActionDto> actions = new java.util.ArrayList<>();

            if (rootNode.isArray()) {
                actions = objectMapper.convertValue(rootNode, new TypeReference<List<AiActionDto>>() {});
            } else if (rootNode.isObject()) {
                AiActionDto singleAction = objectMapper.treeToValue(rootNode, AiActionDto.class);
                actions.add(singleAction);
            }

            if (actions.isEmpty()) {
                return "Команда распознана, но я не понял, что делать с устройствами.";
            }

            // Обновляем БД
            int updatedCount = 0;
            for (AiActionDto action : actions) {
                Optional<Device> deviceOpt = deviceRepository.findById(action.deviceId());
                if (deviceOpt.isPresent()) {
                    Device device = deviceOpt.get();
                    device.setStatus(DeviceStatus.valueOf(action.targetStatus()));
                    if (action.targetValue() != null) {
                        device.setTargetValue(action.targetValue());
                    }
                    deviceRepository.save(device);
                    updatedCount++;
                }
            }

            return "Сделано, сэр. Успешно изменено устройств: " + updatedCount;

        } catch (Exception e) {
            log.error("Ошибка обработки ИИ: {}", e.getMessage(), e);
            return "Простите, сэр. Произошла техническая ошибка в логике.";
        }
    }

    private static @NonNull Map<String, Object> getStringObjectMap(String text, String devicesJson) {
        String systemPrompt = "Ты - REST API сервер умного дома. Твоя единственная задача - генерировать JSON. " +
                "КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО писать программный код (Python, функции def и т.д.), пояснения или текст.\n" +
                "Доступные устройства: " + devicesJson + "\n" +
                "Команда пользователя: '" + text + "'\n" +
                "ПРАВИЛА:\n" +
                "1. Верни массив ТОЛЬКО с теми устройствами, которые нужно изменить (включить/выключить/изменить значение по смыслу команды).\n" +
                "2. 'targetValue' - это число или строго null (маленькими буквами).\n" +
                "ПРИМЕР ТВОЕГО ИДЕАЛЬНОГО ОТВЕТА (скопируй этот формат и больше ничего не пиши):\n" +
                "[\n" +
                "  {\"deviceId\": 3, \"targetStatus\": \"ON\", \"targetValue\": null}\n" +
                "]";

        return Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "system", "content", systemPrompt)),
                "temperature", 0.0
        );
    }
}

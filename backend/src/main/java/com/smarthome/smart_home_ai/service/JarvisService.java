package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.AiActionDto;
import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JarvisService {

    private final ScenarioService scenarioService;
    private final AiClient aiClient;
    private final AiParser aiParser;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    public String processVoiceCommand(String text) {
        String lowerText = text.toLowerCase();

        // 1. Перевіряємо хардкодні сценарії
        Optional<String> scenarioResponse = scenarioService.handleScenario(lowerText);
        return scenarioResponse.orElseGet(() -> handleAiCommand(text));

        // 2. Якщо це не сценарій, відправляємо до ШІ
    }

    private String handleAiCommand(String text) {
        try {
            List<Device> allDevices = deviceRepository.findAll();
            String devicesJson = objectMapper.writeValueAsString(allDevices);

            String rawAiResponse = aiClient.callAi(text, devicesJson);
            List<AiActionDto> actions = aiParser.parseActions(rawAiResponse);

            if (actions.isEmpty()) {
                return "Команду розпізнано, але я не зрозумів, що робити з пристроями.";
            }

            return applyActionsAndGenerateMessage(actions);

        } catch (Exception e) {
            log.error("AI Workflow Error: ", e);
            return "Вибачте, сер. Сталася технічна помилка в логіці розумного будинку.";
        }
    }

    private String applyActionsAndGenerateMessage(List<AiActionDto> actions) {
        List<Device> devicesToUpdate = new ArrayList<>();
        List<String> spokenMessages = new ArrayList<>();

        for (AiActionDto action : actions) {
            Optional<Device> deviceOpt = deviceRepository.findById(action.deviceId());

            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                // Працюємо з Enum замість магічних рядків!
                DeviceStatus newStatus = DeviceStatus.valueOf(action.targetStatus());
                device.setStatus(newStatus);

                if (action.targetValue() != null) {
                    device.setTargetValue(action.targetValue());
                }

                devicesToUpdate.add(device);

                // Формуємо красиву відповідь
                String statusUkr = newStatus == DeviceStatus.ON ? "увімкнено" : "вимкнено";
                if (action.targetValue() != null) {
                    statusUkr = "встановлено на " + action.targetValue();
                }
                spokenMessages.add(device.getName() + " " + statusUkr);
            }
        }

        // Оптимізація бази даних: зберігаємо всі змінені пристрої одним запитом
        if (!devicesToUpdate.isEmpty()) {
            deviceRepository.saveAll(devicesToUpdate);
        }

        return buildFinalMessage(spokenMessages);
    }

    private String buildFinalMessage(List<String> messages) {
        if (messages.isEmpty()) {
            return "Команду розпізнано, але стан пристроїв не змінився.";
        } else if (messages.size() == 1) {
            return "Зроблено, сер. " + messages.getFirst() + ".";
        } else if (messages.size() == 2) {
            return "Зроблено, сер. " + messages.get(0) + " та " + messages.get(1).toLowerCase() + ".";
        } else {
            return "Зроблено, сер. Усі вказані системи успішно оновлено.";
        }
    }
}

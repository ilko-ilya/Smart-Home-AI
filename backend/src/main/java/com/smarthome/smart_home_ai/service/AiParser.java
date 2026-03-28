package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.dto.AiActionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiParser {

    private final ObjectMapper objectMapper;

    public List<AiActionDto> parseActions(String rawContent) {
        try {
            String cleanJson = extractJson(rawContent);
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            List<AiActionDto> actions = new ArrayList<>();

            if (rootNode.isArray()) {
                actions = objectMapper.convertValue(rootNode, new TypeReference<>() {
                });
            } else if (rootNode.isObject()) {
                actions.add(objectMapper.treeToValue(rootNode, AiActionDto.class));
            }
            return actions;

        } catch (Exception e) {
            log.error("Помилка парсингу JSON від ШІ: {}. Сирий текст: {}", e.getMessage(), rawContent);
            return new ArrayList<>(); // Fallback: повертаємо порожній список замість падіння
        }
    }

    private String extractJson(String content) {
        content = content.replace("None", "null").replace("NONE", "null");

        int startArray = content.indexOf('[');
        int startObj = content.indexOf('{');
        int endArray = content.lastIndexOf(']');
        int endObj = content.lastIndexOf('}');

        // Знаходимо перше входження { або [
        int startIndex = -1;
        if (startArray != -1 && startObj != -1) startIndex = Math.min(startArray, startObj);
        else if (startArray != -1) startIndex = startArray;
        else if (startObj != -1) startIndex = startObj;

        // Знаходимо останнє входження } або ]
        int endIndex = Math.max(endArray, endObj);

        if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        throw new RuntimeException("Не знайдено валідний JSON у відповіді");
    }
}

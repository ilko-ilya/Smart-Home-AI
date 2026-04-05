package com.smarthome.smart_home_ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiToolsDefinition {

    public String getToolsJson() {
        // Описуємо інструменти у форматі JSON Schema, який розуміє Groq/Llama 3.1
        return """
        [
          {
            "type": "function",
            "function": {
              "name": "control_devices",
              "description": "Керування пристроями розумного будинку (увімкнути/вимкнути, змінити гучність, яскравість тощо).",
              "parameters": {
                "type": "object",
                "properties": {
                  "actions": {
                    "type": "array",
                    "description": "Список дій для пристроїв",
                    "items": {
                      "type": "object",
                      "properties": {
                        "deviceId": { "type": "integer", "description": "ID пристрою" },
                        "targetStatus": { "type": "string", "enum": ["ON", "OFF"] },
                        "targetValue": { "type": ["integer", "null"], "description": "Числове значення (гучність, температура тощо) або null" }
                      },
                      "required": ["deviceId", "targetStatus"]
                    }
                  }
                },
                "required": ["actions"]
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "search_web",
              "description": "Пошук інформації в інтернеті (новини, факти, погода на вулиці, будь-що, чого ти не знаєш).",
              "parameters": {
                "type": "object",
                "properties": {
                  "query": { "type": "string", "description": "Пошуковий запит для пошуковика" }
                },
                "required": ["query"]
              }
            }
          }
        ]
        """;
    }
}

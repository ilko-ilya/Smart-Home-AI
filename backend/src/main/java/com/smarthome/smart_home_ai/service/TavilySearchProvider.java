package com.smarthome.smart_home_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TavilySearchProvider implements WebSearchProvider {

    private final RestClient restClient;
    @Value("${ai.tavily.api-key:}")
    private String apiKey;

    @Override
    public String search(String query) {
        log.info("Виконується пошук через Tavily: {}", query);
        try {
            JsonNode response = restClient.post()
                    .uri("https://api.tavily.com/search")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "api_key", apiKey,
                            "query", query,
                            "search_depth", "basic",
                            "include_answer", false,
                            "max_results", 1
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("results")) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode result : response.path("results")) {
                    sb.append("- ").append(result.path("content").asString()).append("\n");
                }

                // 🔥 Жорстко обрізаємо текст, якщо він довший за 1000 символів
                String finalResult = sb.toString();
                if (finalResult.length() > 1000) {
                    finalResult = finalResult.substring(0, 1000) + "...";
                }

                return finalResult;
            }
        } catch (Exception e) {
            log.error("Помилка виклику Tavily API: {}", e.getMessage());
            throw new RuntimeException("Tavily недоступний", e);
        }
        return "Немає результатів.";
    }
}

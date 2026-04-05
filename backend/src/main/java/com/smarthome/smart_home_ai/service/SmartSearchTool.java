package com.smarthome.smart_home_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartSearchTool {

    private final TavilySearchProvider tavilyProvider;
    private final DuckDuckGoSearchProvider duckDuckGoProvider;

    // Тот самый счетчик! AtomicInteger потокобезопасный, что хорошо для Spring синглтона
    private final AtomicInteger requestCount = new AtomicInteger(0);

    // Ставим лимит 990, чтобы оставить 10 запросов про запас (лимит Tavily - 1000/мес)
    private static final int TAVILY_MONTHLY_LIMIT = 990;

    public String executeSearch(String query) {
        int currentCount = requestCount.get();

        // Если лимит еще не исчерпан, пробуем Tavily
        if (currentCount < TAVILY_MONTHLY_LIMIT) {
            try {
                String result = tavilyProvider.search(query);
                requestCount.incrementAndGet(); // Увеличиваем счетчик на 1
                log.info("Поиск через Tavily успешен. Счетчик: {}/{}", requestCount.get(), TAVILY_MONTHLY_LIMIT);
                return result;
            } catch (Exception e) {
                // Если Tavily упал (например, отвалился интернет к их серверам или API ключ протух),
                // мы НЕ падаем с ошибкой, а мягко переключаемся на DuckDuckGo
                log.warn("Tavily недоступен (ошибка: {}). Переключаемся на резервный DuckDuckGo.", e.getMessage());
                return duckDuckGoProvider.search(query);
            }
        } else {
            // Если мы превысили 990 запросов в этом месяце, сразу идем в DuckDuckGo
            log.info("Лимит Tavily исчерпан ({} запросов). Используем бесплатный DuckDuckGo.", TAVILY_MONTHLY_LIMIT);
            return duckDuckGoProvider.search(query);
        }
    }

    // Этот метод можно будет потом дергать по @Scheduled крону первого числа каждого месяца,
    // чтобы обнулять счетчик
    public void resetCounter() {
        requestCount.set(0);
        log.info("Счетчик поисковых запросов сброшен на 0.");
    }
}

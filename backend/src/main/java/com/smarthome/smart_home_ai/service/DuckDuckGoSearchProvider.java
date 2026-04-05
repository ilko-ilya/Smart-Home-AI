package com.smarthome.smart_home_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DuckDuckGoSearchProvider implements WebSearchProvider {
    @Override
    public String search(String query) {
        log.info("Виконується пошук через DuckDuckGo: {}", query);
        StringBuilder results = new StringBuilder();
        try {
            String url = "https://lite.duckduckgo.com/lite/";
            Document doc = Jsoup.connect(url)
                    .data("q", query)
                    .post();

            Elements rows = doc.select("tr");
            int count = 0;
            for (Element row : rows) {
                Elements resultSnippet = row.select("td.result-snippet");
                if (!resultSnippet.isEmpty()) {
                    results.append("- ").append(resultSnippet.text()).append("\n");
                    count++;
                }
                if (count >= 5) break; // Беремо топ-5 результатів
            }
        } catch (Exception e) {
            log.error("Помилка парсингу DuckDuckGo: {}", e.getMessage());
            return "Не вдалося знайти інформацію.";
        }
        return results.toString();
    }
}

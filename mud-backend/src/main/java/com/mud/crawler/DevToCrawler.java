package com.mud.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DevToCrawler extends CrawlerBase {

    private static final List<String> TAGS = List.of("ai", "programming", "java", "cpp", "llm", "rag");
    private static final String API_URL_TEMPLATE =
        "https://dev.to/api/articles?tag=%s&per_page=10&top=7";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DevToCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
        this.webClient = WebClient.builder()
            .defaultHeader("User-Agent", "MudBot/1.0")
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting dev.to crawl");

        for (String tag : TAGS) {
            try {
                String json = webClient.get()
                    .uri(API_URL_TEMPLATE.formatted(tag))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (json == null) continue;

                JsonNode articles = objectMapper.readTree(json);
                for (JsonNode article : articles) {
                    String url = article.path("url").asText();
                    if (url.isEmpty()) continue;

                    String urlHash = computeUrlHash(url);
                    if (isDuplicate(urlHash)) continue;

                    String title = article.path("title").asText();
                    String description = article.path("description").asText();
                    String publishedAtStr = article.path("published_at").asText();

                    LocalDateTime publishedAt = LocalDateTime.now();
                    try {
                        publishedAt = LocalDateTime.parse(
                            publishedAtStr,
                            DateTimeFormatter.ISO_DATE_TIME
                        );
                    } catch (Exception ignored) {}

                    TrendItem item = TrendItem.builder()
                        .title(title)
                        .originalUrl(url)
                        .urlHash(urlHash)
                        .source(TrendItem.CrawlSource.DEV_TO)
                        .description(description.isEmpty() ? null : description)
                        .publishedAt(publishedAt)
                        .crawledAt(LocalDateTime.now())
                        .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                        .build();

                    TrendItem saved = saveIfNew(item);
                    if (saved != null) {
                        results.add(saved);
                        log.debug("Saved dev.to article: {}", title);
                    }
                }

                Thread.sleep(500);
            } catch (Exception e) {
                log.error("dev.to crawl failed for tag={}: {}", tag, e.getMessage());
            }
        }

        log.info("dev.to crawl complete: {} new items", results.size());
        return results;
    }
}

package com.mud.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mud.domain.entity.TrendItem.CrawlSource;

@Component
@Slf4j
public class HackerNewsCrawler extends CrawlerBase {

    private static final String TOP_STORIES_URL =
        "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_URL_TEMPLATE =
        "https://hacker-news.firebaseio.com/v0/item/%d.json";
    private static final int MAX_ITEMS = 50;
    private static final int MIN_SCORE = 50;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HackerNewsCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CrawlSource getCrawlSource() {
        return CrawlSource.HACKER_NEWS;
    }

    @Override
    public boolean isScheduledCrawlEnabled() {
        return false; // 개별 30분 스케줄 있음 (HackerNewsCrawlJob)
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Hacker News crawl");

        try {
            // Fetch top story IDs
            List<Integer> storyIds = webClient.get()
                .uri(TOP_STORIES_URL)
                .retrieve()
                .bodyToMono(List.class)
                .block();

            if (storyIds == null || storyIds.isEmpty()) {
                log.warn("No HN story IDs returned");
                return results;
            }

            int fetched = 0;
            for (Object idObj : storyIds) {
                if (fetched >= MAX_ITEMS) break;

                int id = ((Number) idObj).intValue();
                try {
                    Map<String, Object> story = webClient.get()
                        .uri(ITEM_URL_TEMPLATE.formatted(id))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                    if (story == null) continue;

                    String type = (String) story.get("type");
                    Object scoreObj = story.get("score");
                    int score = scoreObj != null ? ((Number) scoreObj).intValue() : 0;

                    if (!"story".equals(type) || score < MIN_SCORE) continue;

                    String url = (String) story.get("url");
                    if (url == null) {
                        // Self post - use HN item URL
                        url = "https://news.ycombinator.com/item?id=" + id;
                    }

                    String urlHash = computeUrlHash(url);
                    if (isDuplicate(urlHash)) {
                        fetched++;
                        continue;
                    }

                    String title = (String) story.get("title");
                    Object timeObj = story.get("time");
                    LocalDateTime publishedAt = timeObj != null
                        ? LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(((Number) timeObj).longValue()),
                            ZoneId.systemDefault())
                        : LocalDateTime.now();

                    TrendItem item = TrendItem.builder()
                        .title(title)
                        .originalUrl(url)
                        .urlHash(urlHash)
                        .source(TrendItem.CrawlSource.HACKER_NEWS)
                        .description("HN Score: " + score)
                        .publishedAt(publishedAt)
                        .crawledAt(LocalDateTime.now())
                        .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                        .build();

                    TrendItem saved = saveIfNew(item);
                    if (saved != null) {
                        results.add(saved);
                        log.debug("Saved HN item: {}", title);
                    }
                    fetched++;

                    Thread.sleep(200); // Respectful rate limiting
                } catch (Exception e) {
                    log.warn("Failed to fetch HN item id={}: {}", id, e.getMessage());
                }
            }

            log.info("HN crawl complete: {} new items", results.size());
        } catch (Exception e) {
            log.error("HN crawl failed", e);
        }

        return results;
    }
}

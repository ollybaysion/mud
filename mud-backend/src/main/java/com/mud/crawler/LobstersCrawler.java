package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LobstersCrawler extends CrawlerBase {

    private static final String RSS_URL = "https://lobste.rs/rss";

    public LobstersCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Lobsters crawl");

        try {
            Document doc = fetchXml(RSS_URL);
            Elements items = doc.select("item");

            for (Element item : items) {
                // Lobsters RSS: <link> is the original URL, <comments> is the discussion URL
                String url = item.select("link").text().trim();
                if (url.isEmpty()) continue;

                // 기술 관련 태그가 있는 것만
                String tags = item.select("category").stream()
                    .map(el -> el.text().toLowerCase())
                    .reduce("", (a, b) -> a + " " + b);

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = item.select("title").text().trim();
                String description = item.select("description").text().trim();
                if (description.length() > 400) description = description.substring(0, 397) + "...";

                String pubDateStr = item.select("pubDate").text().trim();
                LocalDateTime publishedAt = LocalDateTime.now();
                try {
                    publishedAt = ZonedDateTime.parse(pubDateStr,
                        DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
                } catch (Exception ignored) {}

                TrendItem trendItem = TrendItem.builder()
                    .title(title)
                    .originalUrl(url)
                    .urlHash(urlHash)
                    .source(TrendItem.CrawlSource.LOBSTERS)
                    .description(description.isEmpty() ? null : description)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(trendItem);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved Lobsters: {}", title);
                }
            }

            log.info("Lobsters crawl complete: {} new items", results.size());
        } catch (Exception e) {
            log.error("Lobsters crawl failed", e);
        }

        return results;
    }
}

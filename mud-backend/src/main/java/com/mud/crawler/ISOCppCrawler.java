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
public class ISOCppCrawler extends CrawlerBase {

    private static final String RSS_URL = "https://isocpp.org/blog/rss";

    public ISOCppCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting isocpp.org crawl");

        try {
            Document doc = fetchXml(RSS_URL);
            Elements items = doc.select("item");

            for (Element item : items) {
                String url = item.select("link").text().trim();
                if (url.isEmpty()) url = item.select("guid").text().trim();
                if (url.isEmpty()) continue;

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = item.select("title").text().trim();
                if (title.isEmpty()) continue;

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
                    .source(TrendItem.CrawlSource.ISOCPP)
                    .description(description.isEmpty() ? null : description)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(trendItem);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved isocpp: {}", title);
                }
            }

            log.info("isocpp crawl complete: {} new items", results.size());
        } catch (Exception e) {
            log.error("isocpp crawl failed", e);
        }

        return results;
    }
}

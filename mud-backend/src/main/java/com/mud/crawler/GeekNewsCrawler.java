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

/**
 * GeekNews (news.hada.io) - 한국 개발자 대상 기술 뉴스 큐레이션
 * HackerNews 스타일의 한국어 기술 커뮤니티
 */
@Component
@Slf4j
public class GeekNewsCrawler extends CrawlerBase {

    private static final String RSS_URL = "https://feeds.feedburner.com/geeknews-feed";

    public GeekNewsCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting GeekNews crawl");

        try {
            Document doc = fetchXml(RSS_URL);
            Elements entries = doc.select("entry");

            for (Element entry : entries) {
                // Atom: <link href="..." /> 속성에서 URL 추출
                String url = entry.select("link[href]").attr("href").trim();
                if (url.isEmpty()) url = entry.select("id").text().trim();
                if (url.isEmpty()) continue;

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = entry.select("title").text().trim();
                if (title.isEmpty()) continue;

                String description = entry.select("summary").text().trim();
                if (description.isEmpty()) description = entry.select("content").text().trim();

                // Atom: <published> 또는 <updated> (ISO 8601)
                String pubDateStr = entry.select("published").text().trim();
                if (pubDateStr.isEmpty()) pubDateStr = entry.select("updated").text().trim();
                LocalDateTime publishedAt = LocalDateTime.now();
                try {
                    publishedAt = ZonedDateTime.parse(pubDateStr).toLocalDateTime();
                } catch (Exception ignored) {}

                TrendItem trendItem = TrendItem.builder()
                    .title(title)
                    .originalUrl(url)
                    .urlHash(urlHash)
                    .source(TrendItem.CrawlSource.GEEKNEWS)
                    .description(description.isEmpty() ? null : description)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(trendItem);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved GeekNews: {}", title);
                }
            }

            log.info("GeekNews crawl complete: {} new items", results.size());
        } catch (Exception e) {
            log.error("GeekNews crawl failed", e);
        }

        return results;
    }
}

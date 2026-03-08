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
public class HuggingFaceCrawler extends CrawlerBase {

    private static final String RSS_URL = "https://huggingface.co/blog/feed.xml";

    public HuggingFaceCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Hugging Face crawl");

        try {
            Document doc = fetchXml(RSS_URL);

            // Atom feed: <entry> elements
            Elements entries = doc.select("entry");
            for (Element entry : entries) {
                String url = entry.select("link").attr("href").trim();
                if (url.isEmpty()) continue;

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = entry.select("title").text().trim();
                String summary = entry.select("summary").text().trim();
                if (summary.isEmpty()) summary = entry.select("content").text().trim();
                if (summary.length() > 400) summary = summary.substring(0, 397) + "...";

                String publishedStr = entry.select("published").text().trim();
                if (publishedStr.isEmpty()) publishedStr = entry.select("updated").text().trim();
                LocalDateTime publishedAt = LocalDateTime.now();
                try {
                    publishedAt = ZonedDateTime.parse(publishedStr,
                        DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
                } catch (Exception ignored) {}

                TrendItem item = TrendItem.builder()
                    .title(title)
                    .originalUrl(url)
                    .urlHash(urlHash)
                    .source(TrendItem.CrawlSource.HUGGING_FACE)
                    .description(summary.isEmpty() ? null : summary)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(item);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved HuggingFace: {}", title);
                }
            }

            log.info("Hugging Face crawl complete: {} new items", results.size());
        } catch (Exception e) {
            log.error("Hugging Face crawl failed", e);
        }

        return results;
    }
}

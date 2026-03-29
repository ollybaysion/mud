package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.entity.TrendItem.CrawlSource;
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
public class NaverD2Crawler extends CrawlerBase {

    private static final String ATOM_URL = "https://d2.naver.com/d2.atom";

    public NaverD2Crawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public CrawlSource getCrawlSource() {
        return CrawlSource.NAVER_D2;
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Naver D2 Atom crawl");

        try {
            Document doc = fetchXml(ATOM_URL);
            Elements entries = doc.select("entry");

            for (Element entry : entries) {
                Element linkEl = entry.select("link[rel=alternate]").first();
                String url = linkEl != null ? linkEl.attr("href").trim() : "";
                if (url.isEmpty()) continue;

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = entry.select("title").text().trim();
                if (title.isEmpty()) continue;

                String description = cleanDescription(entry.select("content").text());

                LocalDateTime publishedAt = LocalDateTime.now();
                String updated = entry.select("updated").text().trim();
                try {
                    publishedAt = ZonedDateTime.parse(updated, DateTimeFormatter.ISO_DATE_TIME)
                        .toLocalDateTime();
                } catch (Exception ignored) {}

                TrendItem trendItem = TrendItem.builder()
                    .title(title)
                    .originalUrl(url)
                    .urlHash(urlHash)
                    .source(CrawlSource.NAVER_D2)
                    .description(description)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(trendItem);
                if (saved != null) results.add(saved);
            }
        } catch (Exception e) {
            log.error("Naver D2 crawl failed: {}", e.getMessage());
        }

        log.info("Naver D2 crawl complete: {} new items", results.size());
        return results;
    }
}

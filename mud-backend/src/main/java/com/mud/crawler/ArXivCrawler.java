package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.mud.domain.entity.TrendItem.CrawlSource;

@Component
@Slf4j
public class ArXivCrawler extends CrawlerBase {

    private static final String ARXIV_API_URL =
        "https://export.arxiv.org/api/query?search_query=cat:cs.AI+OR+cat:cs.LG+OR+cat:cs.CL&sortBy=submittedDate&sortOrder=descending&max_results=20";

    public ArXivCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public CrawlSource getCrawlSource() {
        return CrawlSource.ARXIV;
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting ArXiv crawl");

        try {
            Document doc = fetchXml(ARXIV_API_URL);
            Elements entries = doc.select("entry");

            for (Element entry : entries) {
                String arxivUrl = entry.select("id").text().trim();
                if (arxivUrl.isEmpty()) continue;

                // Convert to canonical URL: https://arxiv.org/abs/XXXX.XXXXX
                String arxivId = arxivUrl.replaceAll(".*abs/", "").replaceAll("v\\d+$", "");
                String canonicalUrl = "https://arxiv.org/abs/" + arxivId;
                String urlHash = computeUrlHash(canonicalUrl);
                if (isDuplicate(urlHash)) continue;

                String title = entry.select("title").text().trim();
                String summary = entry.select("summary").text().trim();
                String publishedStr = entry.select("published").text().trim();

                LocalDateTime publishedAt = LocalDateTime.now();
                try {
                    publishedAt = LocalDateTime.parse(
                        publishedStr,
                        DateTimeFormatter.ISO_DATE_TIME
                    );
                } catch (Exception ignored) {}

                String description = cleanDescription(summary);

                TrendItem item = TrendItem.builder()
                    .title(title)
                    .originalUrl(canonicalUrl)
                    .urlHash(urlHash)
                    .source(TrendItem.CrawlSource.ARXIV)
                    .description(description)
                    .arxivId(arxivId)
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(item);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved ArXiv paper: {}", title);
                }
            }

            log.info("ArXiv crawl complete: {} new papers", results.size());
        } catch (Exception e) {
            log.error("ArXiv crawl failed", e);
        }

        return results;
    }
}

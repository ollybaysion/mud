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
public class RedditRssCrawler extends CrawlerBase {

    private static final List<String> SUBREDDITS = List.of(
        "programming", "MachineLearning", "cpp", "java", "LocalLLaMA"
    );
    private static final String RSS_URL_TEMPLATE =
        "https://www.reddit.com/r/%s/top/.rss?t=day&limit=15";

    public RedditRssCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Reddit RSS crawl");

        for (String subreddit : SUBREDDITS) {
            try {
                String rssUrl = RSS_URL_TEMPLATE.formatted(subreddit);
                Document doc = fetchXml(rssUrl);
                Elements entries = doc.select("entry");

                for (Element entry : entries) {
                    String url = entry.select("link").attr("href").trim();
                    if (url.isEmpty()) continue;

                    // Remove Reddit tracking params
                    url = url.replaceAll("\\?.*", "");
                    String urlHash = computeUrlHash(url);
                    if (isDuplicate(urlHash)) continue;

                    String title = entry.select("title").text().trim();
                    if (title.isEmpty()) continue;

                    String description = cleanDescription(entry.select("content").text());

                    String publishedStr = entry.select("updated").text().trim();
                    LocalDateTime publishedAt = LocalDateTime.now();
                    try {
                        publishedAt = ZonedDateTime.parse(publishedStr, DateTimeFormatter.ISO_DATE_TIME)
                            .toLocalDateTime();
                    } catch (Exception ignored) {}

                    TrendItem item = TrendItem.builder()
                        .title("[r/" + subreddit + "] " + title)
                        .originalUrl(url)
                        .urlHash(urlHash)
                        .source(TrendItem.CrawlSource.REDDIT)
                        .description(description.isEmpty() ? null : description)
                        .publishedAt(publishedAt)
                        .crawledAt(LocalDateTime.now())
                        .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                        .build();

                    TrendItem saved = saveIfNew(item);
                    if (saved != null) {
                        results.add(saved);
                        log.debug("Saved Reddit item: {}", title);
                    }
                }

                Thread.sleep(1000); // Between subreddits
            } catch (Exception e) {
                log.error("Reddit crawl failed for r/{}: {}", subreddit, e.getMessage());
            }
        }

        log.info("Reddit crawl complete: {} new items", results.size());
        return results;
    }
}

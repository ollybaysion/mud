package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class CrawlerBase {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000;

    protected final TrendItemRepository trendItemRepository;

    protected Document fetchHtml(String url) throws IOException {
        return fetchWithRetry(() -> Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(15_000)
            .get(), url);
    }

    protected Document fetchXml(String url) throws IOException {
        return fetchWithRetry(() -> Jsoup.connect(url)
            .userAgent("Mozilla/5.0 MudBot/1.0")
            .timeout(15_000)
            .parser(Parser.xmlParser())
            .get(), url);
    }

    protected Document fetchXmlBrowser(String url) throws IOException {
        return fetchWithRetry(() -> Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .referrer("https://www.google.com")
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .timeout(15_000)
            .parser(Parser.xmlParser())
            .get(), url);
    }

    Document fetchWithRetry(FetchAction action, String url) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.execute();
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = INITIAL_DELAY_MS * (1L << (attempt - 1));
                    log.warn("Fetch failed (attempt {}/{}), retrying in {}ms: {} - {}",
                        attempt, MAX_RETRIES, delay, url, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        log.error("Fetch failed after {} attempts: {}", MAX_RETRIES, url);
        throw lastException;
    }

    @FunctionalInterface
    interface FetchAction {
        Document execute() throws IOException;
    }

    protected String computeUrlHash(String url) {
        return DigestUtils.md5DigestAsHex(url.getBytes());
    }

    protected boolean isDuplicate(String urlHash) {
        return trendItemRepository.existsByUrlHash(urlHash);
    }

    protected TrendItem saveIfNew(TrendItem item) {
        if (isDuplicate(item.getUrlHash())) {
            log.debug("Skipping duplicate URL: {}", item.getOriginalUrl());
            return null;
        }
        try {
            return trendItemRepository.save(item);
        } catch (Exception e) {
            // Race condition - another thread saved the same URL
            log.debug("Failed to save (likely duplicate): {}", item.getOriginalUrl());
            return null;
        }
    }

    public abstract List<TrendItem> crawl();
}

package com.mud.crawler;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mud.domain.entity.TrendItem.CrawlSource;

@Component
@Slf4j
public class GitHubTrendingCrawler extends CrawlerBase {

    private static final List<String> LANGUAGES = List.of(
        "java", "c++", "python", "javascript", "typescript", "go", "rust"
    );
    private static final String BASE_URL = "https://github.com/trending/";

    public GitHubTrendingCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
    }

    @Override
    public CrawlSource getCrawlSource() {
        return CrawlSource.GITHUB;
    }

    @Override
    public boolean isScheduledCrawlEnabled() {
        return false; // 개별 30분 스케줄 있음 (GitHubCrawlJob)
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting GitHub Trending crawl");

        for (String lang : LANGUAGES) {
            try {
                String langParam = lang.replace("+", "%2B"); // c++ -> c%2B%2B
                Document doc = fetchHtml(BASE_URL + langParam + "?since=daily");
                Elements repos = doc.select("article.Box-row");

                if (repos.isEmpty()) {
                    log.warn("No repos found for lang={} - GitHub may have changed markup", lang);
                }

                for (Element repo : repos) {
                    Element linkEl = repo.selectFirst("h2 a");
                    if (linkEl == null) continue;

                    String repoPath = linkEl.attr("href").trim();
                    String url = "https://github.com" + repoPath;
                    String urlHash = computeUrlHash(url);
                    if (isDuplicate(urlHash)) continue;

                    String title = repoPath.substring(1); // Remove leading /
                    String description = "";
                    Element descEl = repo.selectFirst("p.col-9");
                    if (descEl != null) description = descEl.text().trim();

                    int stars = parseStars(repo);

                    TrendItem item = TrendItem.builder()
                        .title(title)
                        .originalUrl(url)
                        .urlHash(urlHash)
                        .source(TrendItem.CrawlSource.GITHUB)
                        .description(description.isEmpty() ? null : description)
                        .githubStars(stars)
                        .githubLanguage(lang)
                        .publishedAt(LocalDateTime.now())
                        .crawledAt(LocalDateTime.now())
                        .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                        .build();

                    TrendItem saved = saveIfNew(item);
                    if (saved != null) {
                        results.add(saved);
                        log.debug("Saved GitHub repo: {}", title);
                    }
                }

                Thread.sleep(2000); // Polite delay between language pages
            } catch (Exception e) {
                log.error("GitHub trending crawl failed for lang={}: {}", lang, e.getMessage());
            }
        }

        log.info("GitHub Trending crawl complete: {} new repos", results.size());
        return results;
    }

    private int parseStars(Element repo) {
        try {
            Element starsEl = repo.selectFirst("a[href$=stargazers]");
            if (starsEl == null) return 0;
            String text = starsEl.text().replaceAll("[^0-9,]", "").replace(",", "");
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

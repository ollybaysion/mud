package com.mud.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.TrendItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PapersWithCodeCrawler extends CrawlerBase {

    private static final String API_URL =
        "https://paperswithcode.com/api/v1/papers/?ordering=-published&page_size=20";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PapersWithCodeCrawler(TrendItemRepository trendItemRepository) {
        super(trendItemRepository);
        this.webClient = WebClient.builder()
            .defaultHeader("User-Agent", "MudBot/1.0")
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<TrendItem> crawl() {
        List<TrendItem> results = new ArrayList<>();
        log.info("Starting Papers With Code crawl");

        try {
            String json = webClient.get()
                .uri(API_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (json == null) return results;

            JsonNode root = objectMapper.readTree(json);
            JsonNode papers = root.path("results");

            for (JsonNode paper : papers) {
                String arxivId = paper.path("arxiv_id").asText(null);
                String url = arxivId != null
                    ? "https://arxiv.org/abs/" + arxivId
                    : paper.path("url_pdf").asText("");

                if (url.isEmpty()) continue;

                String urlHash = computeUrlHash(url);
                if (isDuplicate(urlHash)) continue;

                String title = paper.path("title").asText();
                String abstract_ = paper.path("abstract").asText("");
                String description = cleanDescription(abstract_);

                // GitHub 구현 코드 수 - 높을수록 주목받는 논문
                int githubRepos = paper.path("repository_count").asInt(0);

                String publishedStr = paper.path("published").asText("");
                LocalDateTime publishedAt = LocalDateTime.now();
                try {
                    publishedAt = LocalDateTime.parse(publishedStr + "T00:00:00",
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ignored) {}

                TrendItem item = TrendItem.builder()
                    .title(title)
                    .originalUrl(url)
                    .urlHash(urlHash)
                    .source(TrendItem.CrawlSource.PAPERS_WITH_CODE)
                    .description(description.isEmpty() ? null : description)
                    .arxivId(arxivId)
                    .githubStars(githubRepos > 0 ? githubRepos : null) // repo 수를 stars 컬럼 재활용
                    .publishedAt(publishedAt)
                    .crawledAt(LocalDateTime.now())
                    .analysisStatus(TrendItem.AnalysisStatus.PENDING)
                    .build();

                TrendItem saved = saveIfNew(item);
                if (saved != null) {
                    results.add(saved);
                    log.debug("Saved PapersWithCode: {}", title);
                }
            }

            log.info("Papers With Code crawl complete: {} new papers", results.size());
        } catch (Exception e) {
            log.error("Papers With Code crawl failed", e);
        }

        return results;
    }
}

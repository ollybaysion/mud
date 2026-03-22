package com.mud.repository;

import com.mud.IntegrationTestBase;
import com.mud.domain.entity.Category;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendItemRepositoryTest extends IntegrationTestBase {

    @Autowired private TrendItemRepository trendItemRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Category aiCategory;

    @BeforeEach
    void setUp() {
        trendItemRepository.deleteAll();
        aiCategory = categoryRepository.findBySlug("ai-ml").orElseThrow();
    }

    private TrendItem createItem(String title, TrendItem.CrawlSource source, int score,
                                  TrendItem.AnalysisStatus status) {
        return trendItemRepository.save(TrendItem.builder()
            .title(title)
            .originalUrl("https://example.com/" + title.hashCode())
            .urlHash("hash" + title.hashCode())
            .source(source)
            .category(aiCategory)
            .relevanceScore(score)
            .koreanSummary("요약: " + title)
            .publishedAt(LocalDateTime.now())
            .crawledAt(LocalDateTime.now())
            .analysisStatus(status)
            .build());
    }

    @Test
    @DisplayName("existsByUrlHash — 존재하는 해시")
    void existsByUrlHashTrue() {
        createItem("Test", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        assertThat(trendItemRepository.existsByUrlHash("hash" + "Test".hashCode())).isTrue();
    }

    @Test
    @DisplayName("existsByUrlHash — 존재하지 않는 해시")
    void existsByUrlHashFalse() {
        assertThat(trendItemRepository.existsByUrlHash("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("findWithFilters — 필터 없이 DONE 아이템만")
    void findWithFiltersNoFilter() {
        createItem("Done1", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("Done2", TrendItem.CrawlSource.GITHUB, 3, TrendItem.AnalysisStatus.DONE);
        createItem("Pending", TrendItem.CrawlSource.GITHUB, 5, TrendItem.AnalysisStatus.PENDING);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            null, null, 1, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findWithFilters — minScore 필터")
    void findWithFiltersMinScore() {
        createItem("High", TrendItem.CrawlSource.GITHUB, 5, TrendItem.AnalysisStatus.DONE);
        createItem("Low", TrendItem.CrawlSource.GITHUB, 2, TrendItem.AnalysisStatus.DONE);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            null, null, 4, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("High");
    }

    @Test
    @DisplayName("findWithFilters — 카테고리 필터")
    void findWithFiltersByCategory() {
        createItem("AI Item", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            "ai-ml", null, 1, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findWithFilters — 소스 필터")
    void findWithFiltersBySource() {
        createItem("GH Item", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("HN Item", TrendItem.CrawlSource.HACKER_NEWS, 4, TrendItem.AnalysisStatus.DONE);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            null, "GITHUB", 1, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("GH Item");
    }

    @Test
    @DisplayName("findWithFilters — 키워드 검색 (title)")
    void findWithFiltersByKeyword() {
        createItem("Rust Performance", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("Java Spring", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            null, null, 1, "Rust", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Rust");
    }

    @Test
    @DisplayName("findByAnalysisStatusIn — PENDING + FAILED 조회")
    void findByStatusIn() {
        createItem("Pending", TrendItem.CrawlSource.GITHUB, 0, TrendItem.AnalysisStatus.PENDING);
        createItem("Failed", TrendItem.CrawlSource.GITHUB, 0, TrendItem.AnalysisStatus.FAILED);
        createItem("Done", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);

        List<TrendItem> result = trendItemRepository.findByAnalysisStatusInOrderByCrawledAtAsc(
            List.of(TrendItem.AnalysisStatus.PENDING, TrendItem.AnalysisStatus.FAILED));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("countBySource — 소스별 집계")
    void countBySource() {
        createItem("GH1", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("GH2", TrendItem.CrawlSource.GITHUB, 3, TrendItem.AnalysisStatus.DONE);
        createItem("HN1", TrendItem.CrawlSource.HACKER_NEWS, 4, TrendItem.AnalysisStatus.DONE);

        List<Object[]> result = trendItemRepository.countBySource();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("countByCategory — 카테고리별 집계")
    void countByCategory() {
        createItem("Item1", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);

        List<Object[]> result = trendItemRepository.countByCategory();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("countByAnalysisStatus — 상태별 카운트")
    void countByStatus() {
        createItem("Done1", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("Done2", TrendItem.CrawlSource.GITHUB, 3, TrendItem.AnalysisStatus.DONE);

        long count = trendItemRepository.countByAnalysisStatus(TrendItem.AnalysisStatus.DONE);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("페이지네이션 — page=0, size=1")
    void pagination() {
        createItem("Item1", TrendItem.CrawlSource.GITHUB, 4, TrendItem.AnalysisStatus.DONE);
        createItem("Item2", TrendItem.CrawlSource.GITHUB, 3, TrendItem.AnalysisStatus.DONE);

        Page<TrendItem> result = trendItemRepository.findWithFilters(
            null, null, 1, null, PageRequest.of(0, 1));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}

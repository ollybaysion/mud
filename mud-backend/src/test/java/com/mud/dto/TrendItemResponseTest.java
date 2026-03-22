package com.mud.dto;

import com.mud.domain.entity.Category;
import com.mud.domain.entity.TrendItem;
import com.mud.dto.response.TrendItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendItemResponseTest {

    @Test
    @DisplayName("전체 필드 매핑")
    void fromWithAllFields() {
        Category category = Category.builder()
            .id(1L).slug("ai-ml").displayName("AI/ML").emoji("🤖").sortOrder(1).build();

        TrendItem item = TrendItem.builder()
            .id(1L)
            .title("Test Title")
            .originalUrl("https://example.com")
            .urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB)
            .description("desc")
            .koreanSummary("한국어 요약")
            .deepAnalysis("심층 분석")
            .category(category)
            .relevanceScore(5)
            .keywords(List.of("AI", "ML"))
            .publishedAt(LocalDateTime.of(2026, 3, 22, 12, 0))
            .crawledAt(LocalDateTime.of(2026, 3, 22, 13, 0))
            .githubStars(1000)
            .githubLanguage("Python")
            .build();

        TrendItemResponse response = TrendItemResponse.from(item);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.source()).isEqualTo("GITHUB");
        assertThat(response.category().slug()).isEqualTo("ai-ml");
        assertThat(response.relevanceScore()).isEqualTo(5);
        assertThat(response.keywords()).containsExactly("AI", "ML");
        assertThat(response.deepAnalysis()).isEqualTo("심층 분석");
        assertThat(response.githubStars()).isEqualTo(1000);
    }

    @Test
    @DisplayName("카테고리 null → response.category null")
    void fromWithNullCategory() {
        TrendItem item = TrendItem.builder()
            .id(2L)
            .title("No Category")
            .originalUrl("https://example.com/2")
            .urlHash("hash2")
            .source(TrendItem.CrawlSource.HACKER_NEWS)
            .crawledAt(LocalDateTime.now())
            .build();

        TrendItemResponse response = TrendItemResponse.from(item);
        assertThat(response.category()).isNull();
        assertThat(response.source()).isEqualTo("HACKER_NEWS");
    }

    @Test
    @DisplayName("keywords 복사 — 원본과 독립")
    void keywordsAreCopied() {
        List<String> original = new java.util.ArrayList<>(List.of("A", "B"));
        TrendItem item = TrendItem.builder()
            .id(3L)
            .title("Copy Test")
            .originalUrl("https://example.com/3")
            .urlHash("hash3")
            .source(TrendItem.CrawlSource.DEV_TO)
            .keywords(original)
            .crawledAt(LocalDateTime.now())
            .build();

        TrendItemResponse response = TrendItemResponse.from(item);
        original.add("C");

        assertThat(response.keywords()).hasSize(2);
    }
}

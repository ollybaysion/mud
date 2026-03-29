package com.mud.service;

import com.mud.domain.entity.Category;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import com.mud.dto.request.TrendFilterRequest;
import com.mud.dto.response.TrendItemResponse;
import com.mud.dto.response.TrendPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendServiceTest {

    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private TrendService trendService;

    private TrendItem createTestItem(Long id, String title) {
        return TrendItem.builder()
            .id(id)
            .title(title)
            .originalUrl("https://example.com/" + id)
            .urlHash("hash" + id)
            .source(TrendItem.CrawlSource.GITHUB)
            .publishedAt(LocalDateTime.now())
            .crawledAt(LocalDateTime.now())
            .analysisStatus(TrendItem.AnalysisStatus.DONE)
            .relevanceScore(4)
            .koreanSummary("요약 " + id)
            .build();
    }

    @Test
    @DisplayName("getTrends — 기본 페이지네이션")
    void getTrendsDefaultPagination() {
        TrendFilterRequest filter = TrendFilterRequest.builder().build();
        Page<TrendItem> page = new PageImpl<>(
            List.of(createTestItem(1L, "Test")),
            PageRequest.of(0, 20), 1
        );

        when(trendItemRepository.findWithFilters(isNull(), isNull(), eq(25), isNull(), any()))
            .thenReturn(page);

        TrendPageResponse result = trendService.getTrends(filter);
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTrendDetail — 존재하는 아이템")
    void getTrendDetailFound() {
        TrendItem item = createTestItem(1L, "Found Item");
        when(trendItemRepository.findById(1L)).thenReturn(Optional.of(item));

        TrendItemResponse result = trendService.getTrendDetail(1L);
        assertThat(result.title()).isEqualTo("Found Item");
        assertThat(result.source()).isEqualTo("GITHUB");
    }

    @Test
    @DisplayName("getTrendDetail — 없는 아이템 → 예외")
    void getTrendDetailNotFound() {
        when(trendItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trendService.getTrendDetail(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getCategories — 정렬 순서대로 반환")
    void getCategoriesSorted() {
        List<Category> categories = List.of(
            Category.builder().id(1L).slug("ai-ml").displayName("AI/ML").emoji("🤖").sortOrder(1).build(),
            Category.builder().id(2L).slug("java").displayName("Java").emoji("☕").sortOrder(5).build()
        );
        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(categories);

        var result = trendService.getCategories();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).slug()).isEqualTo("ai-ml");
        assertThat(result.get(1).slug()).isEqualTo("java");
    }

    @Test
    @DisplayName("getStats — 통계 집계")
    void getStats() {
        when(trendItemRepository.countByAnalysisStatus(TrendItem.AnalysisStatus.DONE)).thenReturn(100L);

        List<Object[]> sourceStats = new java.util.ArrayList<>();
        sourceStats.add(new Object[]{"GITHUB", 50L});
        sourceStats.add(new Object[]{"HACKER_NEWS", 30L});
        when(trendItemRepository.countBySource()).thenReturn(sourceStats);

        List<Object[]> categoryStats = new java.util.ArrayList<>();
        categoryStats.add(new Object[]{"ai-ml", 40L});
        when(trendItemRepository.countByCategory()).thenReturn(categoryStats);

        var result = trendService.getStats();
        assertThat(result.totalItems()).isEqualTo(100);
        assertThat(result.itemsBySource()).containsEntry("GITHUB", 50L);
        assertThat(result.itemsByCategory()).containsEntry("ai-ml", 40L);
    }
}

package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.Category;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.entity.WeeklyReport;
import com.mud.domain.repository.TrendItemRepository;
import com.mud.domain.repository.WeeklyReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock private WeeklyReportRepository weeklyReportRepository;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private WebClient claudeWebClient;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private CacheManager cacheManager;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private WeeklyReportService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "claudeModel", "test-model");
    }

    @Test
    @DisplayName("이미 존재하는 리포트 → 스킵")
    void skipExistingReport() {
        when(weeklyReportRepository.existsByPeriodStartAndPeriodEnd(any(), any())).thenReturn(true);

        service.generateForPeriod(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("getLatestReport — 최신 리포트 반환")
    void getLatestReport() {
        WeeklyReport report = WeeklyReport.builder()
            .id(1L).periodStart(LocalDate.of(2026, 3, 16)).periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(100).highlightsJson("[]").generatedAt(LocalDateTime.now()).build();

        when(weeklyReportRepository.findTopByOrderByPeriodStartDesc()).thenReturn(Optional.of(report));

        Optional<WeeklyReport> result = service.getLatestReport();
        assertThat(result).isPresent();
        assertThat(result.get().getTotalCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("getLatestReport — 리포트 없으면 empty")
    void getLatestReportEmpty() {
        when(weeklyReportRepository.findTopByOrderByPeriodStartDesc()).thenReturn(Optional.empty());

        Optional<WeeklyReport> result = service.getLatestReport();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getReportByDate — 날짜로 조회")
    void getReportByDate() {
        WeeklyReport report = WeeklyReport.builder()
            .id(1L).periodStart(LocalDate.of(2026, 3, 16)).periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(50).highlightsJson("[]").generatedAt(LocalDateTime.now()).build();

        when(weeklyReportRepository.findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(any(), any()))
            .thenReturn(Optional.of(report));

        Optional<WeeklyReport> result = service.getReportByDate(LocalDate.of(2026, 3, 19));
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("getOrGenerateReport — 존재하면 반환")
    void getOrGenerateExisting() {
        WeeklyReport report = WeeklyReport.builder()
            .id(1L).periodStart(LocalDate.of(2026, 3, 16)).periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(50).highlightsJson("[]").generatedAt(LocalDateTime.now()).build();

        when(weeklyReportRepository.findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(any(), any()))
            .thenReturn(Optional.of(report));

        WeeklyReport result = service.getOrGenerateReport(LocalDate.of(2026, 3, 19));
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(50);
        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateForPeriod — 아이템 0건이면 빈 하이라이트로 생성")
    void generateWithNoItems() {
        when(weeklyReportRepository.existsByPeriodStartAndPeriodEnd(any(), any())).thenReturn(false);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(trendItemRepository.findByStatusAndPeriodWithCategory(any(), any(), any()))
            .thenReturn(List.of());

        service.generateForPeriod(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));

        verify(weeklyReportRepository).save(any());
    }

    @Test
    @DisplayName("buildCategoryStats — 카테고리별 통계 계산")
    void buildCategoryStats() {
        Category aiCat = Category.builder().id(1L).slug("ai-ml").displayName("AI/ML").build();
        List<TrendItem> items = List.of(
            TrendItem.builder().id(1L).title("A").originalUrl("u1").urlHash("h1")
                .source(TrendItem.CrawlSource.GITHUB).category(aiCat).relevanceScore(4)
                .crawledAt(LocalDateTime.now()).build(),
            TrendItem.builder().id(2L).title("B").originalUrl("u2").urlHash("h2")
                .source(TrendItem.CrawlSource.GITHUB).category(aiCat).relevanceScore(5)
                .crawledAt(LocalDateTime.now()).build()
        );

        var result = ReflectionTestUtils.invokeMethod(service, "buildCategoryStats", items);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("generateAiSummary — 빈 하이라이트 → 기본 메시지")
    void aiSummaryEmptyHighlights() {
        String result = ReflectionTestUtils.invokeMethod(service, "generateAiSummary",
            LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), List.of());
        assertThat(result).isEqualTo("이번 주에는 수집된 트렌드가 없습니다.");
    }
}

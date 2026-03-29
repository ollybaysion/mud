package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.entity.WeeklyReport;
import com.mud.domain.repository.TrendItemRepository;
import com.mud.domain.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final TrendItemRepository trendItemRepository;
    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final PlatformTransactionManager transactionManager;

    @Value("${claude.api.model}")
    private String claudeModel;

    public void generateWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate periodEnd = periodStart.plusDays(6);

        generateForPeriod(periodStart, periodEnd);
    }

    public void generateForPeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (weeklyReportRepository.existsByPeriodStartAndPeriodEnd(periodStart, periodEnd)) {
            log.info("주간 리포트 이미 존재: {} ~ {}", periodStart, periodEnd);
            return;
        }

        log.info("주간 리포트 생성 시작: {} ~ {}", periodStart, periodEnd);

        LocalDateTime startDt = periodStart.atStartOfDay();
        LocalDateTime endDt = periodEnd.atTime(23, 59, 59);

        // 1단계: 데이터 조회 (트랜잭션)
        TransactionTemplate txRead = new TransactionTemplate(transactionManager);
        txRead.setReadOnly(true);
        List<TrendItem> periodItems = txRead.execute(status ->
            trendItemRepository.findByStatusAndPeriodWithCategory(
                TrendItem.AnalysisStatus.DONE, startDt, endDt));
        int totalCount = periodItems.size();

        List<TrendItem> highlights = periodItems.stream()
            .filter(item -> item.getScoreTotal() != null && item.getScoreTotal() >= 65)
            .limit(15)
            .toList();

        Map<String, Map<String, Object>> categoryStats = buildCategoryStats(periodItems);

        try {
            String highlightsJson = objectMapper.writeValueAsString(highlights.stream().map(item -> Map.of(
                "id", item.getId(),
                "title", item.getTitle(),
                "source", item.getSource().name(),
                "scoreTotal", item.getScoreTotal() != null ? item.getScoreTotal() : 0,
                "relevanceScore", item.getRelevanceScore() != null ? item.getRelevanceScore() : 0,
                "categorySlug", item.getCategory() != null ? item.getCategory().getSlug() : "general",
                "koreanSummary", item.getKoreanSummary() != null ? item.getKoreanSummary() : "",
                "originalUrl", item.getOriginalUrl()
            )).toList());

            String categoryStatsJson = objectMapper.writeValueAsString(categoryStats);

            // 2단계: Claude API 호출 (트랜잭션 밖)
            String aiSummary = generateAiSummary(periodStart, periodEnd, highlights);

            // 3단계: 결과 저장 (트랜잭션)
            TransactionTemplate txWrite = new TransactionTemplate(transactionManager);
            txWrite.executeWithoutResult(status -> {
                WeeklyReport report = WeeklyReport.builder()
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .totalCount(totalCount)
                    .highlightsJson(highlightsJson)
                    .categoryStatsJson(categoryStatsJson)
                    .aiSummary(aiSummary)
                    .aiModel(claudeModel)
                    .generatedAt(LocalDateTime.now())
                    .build();
                weeklyReportRepository.save(report);
            });

            evictWeeklyReportCache();
            log.info("주간 리포트 생성 완료: {} ~ {}, 총 {}건, 하이라이트 {}건",
                periodStart, periodEnd, totalCount, highlights.size());

        } catch (Exception e) {
            log.error("주간 리포트 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주간 리포트 생성 실패", e);
        }
    }

    private Map<String, Map<String, Object>> buildCategoryStats(List<TrendItem> items) {
        Map<String, Map<String, Object>> stats = new LinkedHashMap<>();
        items.stream()
            .filter(item -> item.getCategory() != null)
            .collect(Collectors.groupingBy(item -> item.getCategory().getSlug()))
            .forEach((slug, catItems) -> {
                double avgScore = catItems.stream()
                    .filter(i -> i.getScoreTotal() != null)
                    .mapToInt(i -> i.getScoreTotal().intValue())
                    .average().orElse(0);
                stats.put(slug, Map.of("count", catItems.size(), "avgScore", Math.round(avgScore * 10) / 10.0));
            });
        return stats;
    }

    private String generateAiSummary(LocalDate periodStart, LocalDate periodEnd, List<TrendItem> highlights) {
        if (highlights.isEmpty()) {
            return "이번 주에는 수집된 트렌드가 없습니다.";
        }

        StringBuilder items = new StringBuilder();
        for (int i = 0; i < highlights.size(); i++) {
            TrendItem item = highlights.get(i);
            items.append("%d. [%s] %s — %s\n".formatted(
                i + 1, item.getSource().name(), item.getTitle(),
                item.getKoreanSummary() != null ? item.getKoreanSummary() : "요약 없음"
            ));
        }

        String prompt = """
            아래는 %s ~ %s 기간의 기술 트렌드 상위 %d건입니다.
            이 트렌드들을 분석하여 한국어로 주간 요약을 작성하세요.

            형식:
            ## 이번 주 핵심
            - 가장 주목할 트렌드 2~3개를 한 문단으로 요약

            ## 카테고리별 동향
            - 주요 카테고리별 1~2문장 정리

            ## 주목할 키워드
            - 이번 주 자주 등장한 키워드 5개와 의미

            %s
            """.formatted(periodStart, periodEnd, highlights.size(), items);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", claudeModel,
                "max_tokens", 2048,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            Map<?, ?> response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(120))
                .block();

            if (response == null) return null;

            List<?> content = (List<?>) response.get("content");
            if (content == null || content.isEmpty()) return null;
            Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
            return (String) firstBlock.get("text");

        } catch (Exception e) {
            log.error("AI 주간 요약 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    private void evictWeeklyReportCache() {
        var cache = cacheManager.getCache("weekly-report");
        if (cache != null) cache.clear();
    }

    @Cacheable(value = "weekly-report")
    @Transactional(readOnly = true)
    public Optional<WeeklyReport> getLatestReport() {
        return weeklyReportRepository.findTopByOrderByPeriodStartDesc();
    }

    @Transactional(readOnly = true)
    public Optional<WeeklyReport> getReportByDate(LocalDate date) {
        return weeklyReportRepository.findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(date, date);
    }

    public WeeklyReport getOrGenerateReport(LocalDate date) {
        LocalDate periodStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate periodEnd = periodStart.plusDays(6);

        return weeklyReportRepository.findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(date, date)
            .orElseGet(() -> {
                generateForPeriod(periodStart, periodEnd);
                return weeklyReportRepository.findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(date, date)
                    .orElse(null);
            });
    }

    public void regenerateReport(LocalDate date) {
        LocalDate periodStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate periodEnd = periodStart.plusDays(6);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status ->
            weeklyReportRepository.deleteByPeriodStartAndPeriodEnd(periodStart, periodEnd));

        generateForPeriod(periodStart, periodEnd);
    }
}

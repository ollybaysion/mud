package com.mud.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.WeeklyReport;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
public record WeeklyReportResponse(
    Long id,
    LocalDate periodStart,
    LocalDate periodEnd,
    int totalCount,
    List<Map<String, Object>> highlights,
    Map<String, Object> categoryStats,
    String aiSummary,
    LocalDateTime generatedAt
) {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static WeeklyReportResponse from(WeeklyReport report) {
        List<Map<String, Object>> highlights = List.of();
        Map<String, Object> categoryStats = Map.of();

        try {
            highlights = mapper.readValue(report.getHighlightsJson(),
                new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("highlights_json 파싱 실패: {}", e.getMessage());
        }

        try {
            if (report.getCategoryStatsJson() != null) {
                categoryStats = mapper.readValue(report.getCategoryStatsJson(),
                    new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("category_stats_json 파싱 실패: {}", e.getMessage());
        }

        return new WeeklyReportResponse(
            report.getId(),
            report.getPeriodStart(),
            report.getPeriodEnd(),
            report.getTotalCount(),
            highlights,
            categoryStats,
            report.getAiSummary(),
            report.getGeneratedAt()
        );
    }
}

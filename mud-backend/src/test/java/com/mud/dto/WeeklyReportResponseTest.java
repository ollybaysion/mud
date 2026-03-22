package com.mud.dto;

import com.mud.domain.entity.WeeklyReport;
import com.mud.dto.response.WeeklyReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportResponseTest {

    @Test
    @DisplayName("WeeklyReport → WeeklyReportResponse 매핑")
    void fromReport() {
        WeeklyReport report = WeeklyReport.builder()
            .id(1L)
            .periodStart(LocalDate.of(2026, 3, 16))
            .periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(100)
            .highlightsJson("[{\"id\":1,\"title\":\"Test\"}]")
            .categoryStatsJson("{\"ai-ml\":{\"count\":10,\"avgScore\":4.2}}")
            .aiSummary("## 이번 주 핵심\n테스트")
            .generatedAt(LocalDateTime.now())
            .build();

        WeeklyReportResponse response = WeeklyReportResponse.from(report);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.totalCount()).isEqualTo(100);
        assertThat(response.highlights()).hasSize(1);
        assertThat(response.categoryStats()).containsKey("ai-ml");
        assertThat(response.aiSummary()).contains("핵심");
    }

    @Test
    @DisplayName("잘못된 JSON → 빈 리스트/맵 반환")
    void fromReportInvalidJson() {
        WeeklyReport report = WeeklyReport.builder()
            .id(2L)
            .periodStart(LocalDate.of(2026, 3, 16))
            .periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(0)
            .highlightsJson("invalid json")
            .categoryStatsJson("also invalid")
            .generatedAt(LocalDateTime.now())
            .build();

        WeeklyReportResponse response = WeeklyReportResponse.from(report);

        assertThat(response.highlights()).isEmpty();
        assertThat(response.categoryStats()).isEmpty();
    }

    @Test
    @DisplayName("categoryStatsJson null → 빈 맵")
    void fromReportNullCategoryStats() {
        WeeklyReport report = WeeklyReport.builder()
            .id(3L)
            .periodStart(LocalDate.of(2026, 3, 16))
            .periodEnd(LocalDate.of(2026, 3, 22))
            .totalCount(0)
            .highlightsJson("[]")
            .categoryStatsJson(null)
            .generatedAt(LocalDateTime.now())
            .build();

        WeeklyReportResponse response = WeeklyReportResponse.from(report);
        assertThat(response.categoryStats()).isEmpty();
    }
}

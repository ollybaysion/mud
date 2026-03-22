package com.mud.domain;

import com.mud.domain.entity.TrendItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TrendItemTest {

    @Test
    @DisplayName("빌더 기본값 — analysisStatus=PENDING")
    void builderDefaults() {
        TrendItem item = TrendItem.builder()
            .title("Test")
            .originalUrl("https://example.com")
            .urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB)
            .crawledAt(LocalDateTime.now())
            .analysisStatus(TrendItem.AnalysisStatus.PENDING)
            .build();

        assertThat(item.getAnalysisStatus()).isEqualTo(TrendItem.AnalysisStatus.PENDING);
    }

    @Test
    @DisplayName("CrawlSource enum — 모든 값 유효")
    void allCrawlSourcesValid() {
        assertThat(TrendItem.CrawlSource.values().length).isGreaterThanOrEqualTo(28);
        assertThat(TrendItem.CrawlSource.valueOf("GITHUB")).isEqualTo(TrendItem.CrawlSource.GITHUB);
        assertThat(TrendItem.CrawlSource.valueOf("NVIDIA_BLOG")).isEqualTo(TrendItem.CrawlSource.NVIDIA_BLOG);
        assertThat(TrendItem.CrawlSource.valueOf("PHORONIX")).isEqualTo(TrendItem.CrawlSource.PHORONIX);
    }

    @Test
    @DisplayName("AnalysisStatus enum — 상태 전이 값 확인")
    void analysisStatusValues() {
        assertThat(TrendItem.AnalysisStatus.values())
            .containsExactly(
                TrendItem.AnalysisStatus.PENDING,
                TrendItem.AnalysisStatus.PROCESSING,
                TrendItem.AnalysisStatus.DONE,
                TrendItem.AnalysisStatus.FAILED
            );
    }
}

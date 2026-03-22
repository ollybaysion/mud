package com.mud.service;

import com.mud.domain.entity.TrendItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServicePromptTest {

    private Object service;
    private Method buildBatchPrompt;
    private Method buildDeepAnalysisPrompt;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = AnalysisService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        service = constructor.newInstance(null, null, null, null, null, null, null, null);

        buildBatchPrompt = AnalysisService.class.getDeclaredMethod("buildBatchPrompt", List.class);
        buildBatchPrompt.setAccessible(true);

        buildDeepAnalysisPrompt = AnalysisService.class.getDeclaredMethod("buildDeepAnalysisPrompt", TrendItem.class);
        buildDeepAnalysisPrompt.setAccessible(true);
    }

    private TrendItem createItem(String title, String desc) {
        return TrendItem.builder()
            .title(title)
            .originalUrl("https://example.com/" + title)
            .urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB)
            .description(desc)
            .koreanSummary("요약")
            .keywords(List.of("AI", "ML"))
            .crawledAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("배치 프롬프트 — 아이템 번호 포함")
    void batchPromptContainsItemNumbers() throws Exception {
        List<TrendItem> batch = List.of(
            createItem("Title A", "Desc A"),
            createItem("Title B", "Desc B")
        );

        String prompt = (String) buildBatchPrompt.invoke(service, batch);
        assertThat(prompt).contains("항목 1").contains("항목 2");
        assertThat(prompt).contains("Title A").contains("Title B");
        assertThat(prompt).contains("2개의 기술 트렌드");
    }

    @Test
    @DisplayName("배치 프롬프트 — null description → '설명 없음'")
    void batchPromptNullDescription() throws Exception {
        List<TrendItem> batch = List.of(createItem("Title", null));

        String prompt = (String) buildBatchPrompt.invoke(service, batch);
        assertThat(prompt).contains("설명 없음");
    }

    @Test
    @DisplayName("배치 프롬프트 — categorySlug 선택지에 hardware 포함")
    void batchPromptContainsHardwareCategory() throws Exception {
        List<TrendItem> batch = List.of(createItem("Title", "desc"));

        String prompt = (String) buildBatchPrompt.invoke(service, batch);
        assertThat(prompt).contains("hardware");
    }

    @Test
    @DisplayName("심층분석 프롬프트 — 필수 섹션 포함")
    void deepAnalysisPromptContainsSections() throws Exception {
        TrendItem item = createItem("Deep Title", "Deep Desc");

        String prompt = (String) buildDeepAnalysisPrompt.invoke(service, item);
        assertThat(prompt).contains("기술 개요").contains("핵심 내용")
            .contains("중요성 및 영향").contains("실무 적용 가능성").contains("관련 기술");
    }

    @Test
    @DisplayName("심층분석 프롬프트 — null 필드 fallback")
    void deepAnalysisPromptNullFields() throws Exception {
        TrendItem item = TrendItem.builder()
            .title("Title")
            .originalUrl("https://example.com")
            .urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB)
            .crawledAt(LocalDateTime.now())
            .build();

        String prompt = (String) buildDeepAnalysisPrompt.invoke(service, item);
        assertThat(prompt).contains("설명 없음").contains("요약 없음");
    }
}

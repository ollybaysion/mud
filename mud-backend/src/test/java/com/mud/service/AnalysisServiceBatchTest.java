package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisServiceBatchTest {

    private Object service;
    private Method callClaudeApiBatch;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = AnalysisService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        // WebClient is null — callClaudeApiBatch will NPE, but we test extractResponseText and parseBatchResult indirectly
        service = constructor.newInstance(null, null, null, new ObjectMapper(), null, null, null, null);
    }

    private TrendItem createItem(Long id, String title) {
        return TrendItem.builder()
            .id(id).title(title)
            .originalUrl("https://example.com/" + id)
            .urlHash("hash" + id)
            .source(TrendItem.CrawlSource.GITHUB)
            .description("desc")
            .crawledAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("parseBatchResult — 여러 아이템 파싱")
    void parseMultipleItems() throws Exception {
        Method parse = AnalysisService.class.getDeclaredMethod("parseBatchResult", String.class, int.class);
        parse.setAccessible(true);

        String json = """
            [
              {"koreanSummary": "요약1", "categorySlug": "ai-ml", "relevanceScore": 5, "keywords": ["AI"]},
              {"koreanSummary": "요약2", "categorySlug": "java", "relevanceScore": 3, "keywords": ["Spring"]},
              {"koreanSummary": "요약3", "categorySlug": "devops", "relevanceScore": 4, "keywords": ["K8s", "Docker"]}
            ]
            """;

        List<?> results = (List<?>) parse.invoke(service, json, 3);
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("parseBatchResult — 빈 keywords 배열")
    void parseEmptyKeywords() throws Exception {
        Method parse = AnalysisService.class.getDeclaredMethod("parseBatchResult", String.class, int.class);
        parse.setAccessible(true);

        String json = """
            [{"koreanSummary": "요약", "categorySlug": "general", "relevanceScore": 2, "keywords": []}]
            """;

        List<?> results = (List<?>) parse.invoke(service, json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("extractResponseText — 여러 content 블록 중 첫 번째 사용")
    void extractFirstContentBlock() throws Exception {
        Method extract = AnalysisService.class.getDeclaredMethod("extractResponseText", Map.class);
        extract.setAccessible(true);

        Map<String, Object> response = Map.of(
            "content", List.of(
                Map.of("type", "text", "text", "첫 번째"),
                Map.of("type", "text", "text", "두 번째")
            )
        );

        String result = (String) extract.invoke(service, response);
        assertThat(result).isEqualTo("첫 번째");
    }

    @Test
    @DisplayName("buildBatchPrompt — CrawlSource 이름 포함")
    void batchPromptIncludesSource() throws Exception {
        Method build = AnalysisService.class.getDeclaredMethod("buildBatchPrompt", List.class);
        build.setAccessible(true);

        TrendItem item = createItem(1L, "GitHub Trending");
        String prompt = (String) build.invoke(service, List.of(item));
        assertThat(prompt).contains("GITHUB");
    }
}

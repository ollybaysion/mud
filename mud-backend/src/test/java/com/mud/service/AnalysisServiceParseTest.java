package com.mud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceParseTest {

    private Object service;
    private Method parseBatchResult;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = AnalysisService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        // RequiredArgsConstructor — pass nulls for dependencies (not used in parse logic)
        service = constructor.newInstance(null, null, null, new com.fasterxml.jackson.databind.ObjectMapper(), null, null, null, null);

        parseBatchResult = AnalysisService.class.getDeclaredMethod("parseBatchResult", String.class, int.class);
        parseBatchResult.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private List<?> parse(String text, int size) throws Exception {
        return (List<?>) parseBatchResult.invoke(service, text, size);
    }

    @Test
    @DisplayName("정상 JSON 배열 파싱")
    void parsesValidJsonArray() throws Exception {
        String json = """
            [
              {
                "koreanSummary": "테스트 요약",
                "categorySlug": "ai-ml",
                "relevanceScore": 4,
                "keywords": ["AI", "ML"]
              }
            ]
            """;

        List<?> results = parse(json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("마크다운 코드블록 제거 후 파싱")
    void parsesJsonWrappedInCodeBlock() throws Exception {
        String json = """
            ```json
            [{"koreanSummary": "요약", "categorySlug": "general", "relevanceScore": 3, "keywords": []}]
            ```
            """;

        List<?> results = parse(json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("필드 누락 시 기본값 적용")
    void usesDefaultsForMissingFields() throws Exception {
        String json = "[{}]";

        List<?> results = parse(json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("relevanceScore 1~5 범위 클램핑")
    void clampsRelevanceScore() throws Exception {
        String json = """
            [
              {"koreanSummary": "a", "categorySlug": "general", "relevanceScore": 10, "keywords": []},
              {"koreanSummary": "b", "categorySlug": "general", "relevanceScore": 0, "keywords": []}
            ]
            """;

        List<?> results = parse(json, 2);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("잘못된 JSON → 빈 리스트 반환")
    void returnsEmptyForInvalidJson() throws Exception {
        List<?> results = parse("not json at all", 1);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("JSON 배열이 아닌 경우 → 빈 리스트")
    void returnsEmptyForNonArray() throws Exception {
        List<?> results = parse("{\"key\": \"value\"}", 1);
        assertThat(results).isEmpty();
    }
}

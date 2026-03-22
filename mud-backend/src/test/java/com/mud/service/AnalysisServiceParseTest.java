package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceParseTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    private List<?> parse(String text, int size) {
        return ReflectionTestUtils.invokeMethod(service, "parseBatchResult", text, size);
    }

    @Test
    @DisplayName("정상 JSON 배열 파싱")
    void parsesValidJsonArray() {
        String json = """
            [{"koreanSummary": "테스트 요약", "categorySlug": "ai-ml", "relevanceScore": 4, "keywords": ["AI", "ML"]}]
            """;
        assertThat(parse(json, 1)).hasSize(1);
    }

    @Test
    @DisplayName("마크다운 코드블록 제거 후 파싱")
    void parsesJsonWrappedInCodeBlock() {
        String json = """
            ```json
            [{"koreanSummary": "요약", "categorySlug": "general", "relevanceScore": 3, "keywords": []}]
            ```
            """;
        assertThat(parse(json, 1)).hasSize(1);
    }

    @Test
    @DisplayName("필드 누락 시 기본값 적용")
    void usesDefaultsForMissingFields() {
        assertThat(parse("[{}]", 1)).hasSize(1);
    }

    @Test
    @DisplayName("relevanceScore 1~5 범위 클램핑")
    void clampsRelevanceScore() {
        String json = """
            [
              {"koreanSummary": "a", "categorySlug": "general", "relevanceScore": 10, "keywords": []},
              {"koreanSummary": "b", "categorySlug": "general", "relevanceScore": 0, "keywords": []}
            ]
            """;
        assertThat(parse(json, 2)).hasSize(2);
    }

    @Test
    @DisplayName("잘못된 JSON → 빈 리스트 반환")
    void returnsEmptyForInvalidJson() {
        assertThat(parse("not json at all", 1)).isEmpty();
    }

    @Test
    @DisplayName("JSON 배열이 아닌 경우 → 빈 리스트")
    void returnsEmptyForNonArray() {
        assertThat(parse("{\"key\": \"value\"}", 1)).isEmpty();
    }
}

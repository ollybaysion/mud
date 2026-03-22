package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceBatchTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    @Test
    @DisplayName("parseBatchResult — 여러 아이템 파싱")
    void parseMultipleItems() {
        String json = """
            [
              {"koreanSummary": "요약1", "categorySlug": "ai-ml", "relevanceScore": 5, "keywords": ["AI"]},
              {"koreanSummary": "요약2", "categorySlug": "java", "relevanceScore": 3, "keywords": ["Spring"]},
              {"koreanSummary": "요약3", "categorySlug": "devops", "relevanceScore": 4, "keywords": ["K8s", "Docker"]}
            ]
            """;
        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 3);
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("parseBatchResult — 빈 keywords 배열")
    void parseEmptyKeywords() {
        String json = """
            [{"koreanSummary": "요약", "categorySlug": "general", "relevanceScore": 2, "keywords": []}]
            """;
        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("extractResponseText — 여러 content 블록 중 첫 번째 사용")
    void extractFirstContentBlock() {
        Map<String, Object> response = Map.of(
            "content", List.of(
                Map.of("type", "text", "text", "첫 번째"),
                Map.of("type", "text", "text", "두 번째")
            )
        );
        String result = ReflectionTestUtils.invokeMethod(service, "extractResponseText", response);
        assertThat(result).isEqualTo("첫 번째");
    }

    @Test
    @DisplayName("buildBatchPrompt — CrawlSource 이름 포함")
    void batchPromptIncludesSource() {
        TrendItem item = TrendItem.builder()
            .id(1L).title("GitHub Trending").originalUrl("https://example.com/1")
            .urlHash("hash1").source(TrendItem.CrawlSource.GITHUB).description("desc")
            .crawledAt(LocalDateTime.now()).build();
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildBatchPrompt", List.of(item));
        assertThat(prompt).contains("GITHUB");
    }
}

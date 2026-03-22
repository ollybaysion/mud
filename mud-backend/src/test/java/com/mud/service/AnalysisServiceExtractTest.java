package com.mud.service;

import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceExtractTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    private String extract(Map<?, ?> response) {
        return ReflectionTestUtils.invokeMethod(service, "extractResponseText", response);
    }

    @Test
    @DisplayName("정상 응답에서 텍스트 추출")
    void extractsTextFromValidResponse() {
        Map<String, Object> response = Map.of(
            "content", List.of(Map.of("type", "text", "text", "분석 결과"))
        );
        assertThat(extract(response)).isEqualTo("분석 결과");
    }

    @Test
    @DisplayName("content가 빈 리스트 → RuntimeException")
    void throwsOnEmptyContent() {
        Map<String, Object> response = Map.of("content", List.of());
        assertThatThrownBy(() -> extract(response)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("여러 content 블록 중 첫 번째 사용")
    void extractFirstContentBlock() {
        Map<String, Object> response = Map.of(
            "content", List.of(
                Map.of("type", "text", "text", "첫 번째"),
                Map.of("type", "text", "text", "두 번째")
            )
        );
        assertThat(extract(response)).isEqualTo("첫 번째");
    }
}

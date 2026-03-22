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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnalysisServicePartitionTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    @SuppressWarnings("unchecked")
    private <T> List<List<T>> partition(List<T> list, int size) {
        return ReflectionTestUtils.invokeMethod(service, "partition", list, size);
    }

    @Test
    @DisplayName("균등 분할: 10개 → size=5 → [5,5]")
    void evenPartition() {
        List<List<Integer>> result = partition(List.of(1,2,3,4,5,6,7,8,9,10), 5);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(5);
    }

    @Test
    @DisplayName("불균등 분할: 10개 → size=3 → [3,3,3,1]")
    void unevenPartition() {
        List<List<Integer>> result = partition(List.of(1,2,3,4,5,6,7,8,9,10), 3);
        assertThat(result).hasSize(4);
        assertThat(result.get(3)).hasSize(1);
    }

    @Test
    @DisplayName("빈 리스트 → 빈 결과")
    void emptyList() {
        List<List<Integer>> result = partition(List.of(), 5);
        assertThat(result).isEmpty();
    }
}

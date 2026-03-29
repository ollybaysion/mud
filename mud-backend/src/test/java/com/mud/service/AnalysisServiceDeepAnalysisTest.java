package com.mud.service;

import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceDeepAnalysisTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "executor", Executors.newSingleThreadExecutor());
        ReflectionTestUtils.setField(service, "semaphore", new Semaphore(2));
        ReflectionTestUtils.setField(service, "concurrency", 2);
        ReflectionTestUtils.setField(service, "batchSize", 2);
    }

    @Test
    @DisplayName("이미 분석 완료된 아이템 → 캐시된 결과 반환")
    void returnsExistingDeepAnalysis() {
        TrendItem item = TrendItem.builder()
            .id(1L).title("Test").originalUrl("https://example.com").urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB).deepAnalysis("기존 분석 결과")
            .crawledAt(LocalDateTime.now()).build();

        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(trendItemRepository.findById(1L)).thenReturn(Optional.of(item));

        String result = service.generateDeepAnalysis(1L);
        assertThat(result).isEqualTo("기존 분석 결과");
    }

    @Test
    @DisplayName("존재하지 않는 아이템 → IllegalArgumentException")
    void throwsForNonExistentItem() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(trendItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateDeepAnalysis(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }

    @Test
    @DisplayName("analyzePendingItems — 대기 아이템 없으면 즉시 리턴")
    void analyzePendingItemsNoPending() {
        var lock = new java.util.concurrent.locks.ReentrantLock();
        when(redisLockRegistry.obtain("analysis:pending")).thenReturn(lock);
        when(trendItemRepository.findByAnalysisStatusInOrderByCrawledAtAsc(any(), any()))
            .thenReturn(Page.empty());

        service.analyzePendingItems();
    }
}

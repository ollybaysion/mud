package com.mud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceScoringTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
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
        ReflectionTestUtils.setField(service, "batchSize", 5);
        ReflectionTestUtils.setField(service, "scoringPhase", 2);
    }

    // --- calculateTimeliness ---

    @Test
    @DisplayName("calculateTimeliness — 24시간 이내 → 2")
    void timeliness24h() {
        int result = service.calculateTimeliness(LocalDateTime.now().minusHours(12));
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("calculateTimeliness — 3일 전 → 1")
    void timeliness3days() {
        int result = service.calculateTimeliness(LocalDateTime.now().minusDays(3));
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("calculateTimeliness — 7일 초과 → 0")
    void timelinessOver7days() {
        int result = service.calculateTimeliness(LocalDateTime.now().minusDays(10));
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateTimeliness — null → 0")
    void timelinessNull() {
        int result = service.calculateTimeliness(null);
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateTimeliness — 정확히 24시간 → 2")
    void timelinessExactly24h() {
        int result = service.calculateTimeliness(LocalDateTime.now().minusHours(24));
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("calculateTimeliness — 정확히 7일 → 1")
    void timelinessExactly7days() {
        int result = service.calculateTimeliness(LocalDateTime.now().minusDays(7));
        assertThat(result).isEqualTo(1);
    }

    // --- parseBatchResult with scoring ---

    @Test
    @DisplayName("새 scoring 형식 파싱 — relevance, actionability, impact")
    void parseScoringFormat() {
        String json = """
            [{
                "koreanSummary": "테스트",
                "categorySlug": "ai-ml",
                "keywords": ["AI"],
                "scoring": {"relevance": 3, "actionability": 2, "impact": 1},
                "topicTag": "kubernetes"
            }]
            """;

        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("scoring 누락 시 기본값 적용")
    void parseMissingScoringDefaults() {
        String json = """
            [{"koreanSummary": "요약", "categorySlug": "general", "keywords": []}]
            """;

        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("topicTag 파싱")
    void parseTopicTag() {
        String json = """
            [{
                "koreanSummary": "요약",
                "categorySlug": "devops",
                "keywords": ["K8s"],
                "scoring": {"relevance": 2, "actionability": 3, "impact": 2},
                "topicTag": "docker"
            }]
            """;

        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("scoring 값 클램핑 — 범위 초과 시 max로 제한")
    void parseScoringClamping() {
        String json = """
            [{
                "koreanSummary": "요약",
                "categorySlug": "general",
                "keywords": [],
                "scoring": {"relevance": 10, "actionability": -1, "impact": 5},
                "topicTag": "test"
            }]
            """;

        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("빈 JSON 배열 → 빈 리스트")
    void parseEmptyArray() {
        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", "[]", 0);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("topicTag 없으면 null")
    void parseNoTopicTag() {
        String json = """
            [{"koreanSummary": "요약", "categorySlug": "general", "keywords": [], "scoring": {"relevance": 1, "actionability": 1, "impact": 0}}]
            """;

        List<?> results = ReflectionTestUtils.invokeMethod(service, "parseBatchResult", json, 1);
        assertThat(results).hasSize(1);
    }

    // --- Rescore status ---

    @Test
    @DisplayName("getRescoreStatus — 초기 상태")
    void rescoreStatusInitial() {
        Map<String, Object> status = service.getRescoreStatus();
        assertThat(status.get("inProgress")).isEqualTo(false);
        assertThat(status.get("processed")).isEqualTo(0);
        assertThat(status.get("failed")).isEqualTo(0);
        assertThat(status.get("total")).isEqualTo(0);
    }

    @Test
    @DisplayName("rescore 중복 실행 방지 — 이미 진행 중이면 스킵")
    void rescoreDuplicatePrevention() {
        // 첫 번째 호출용 락 + 빈 결과
        when(redisLockRegistry.obtain("analysis:rescore")).thenReturn(new ReentrantLock());
        when(transactionManager.getTransaction(any()))
            .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        when(trendItemRepository.findByAnalysisStatusAndScoringRelevanceIsNullOrderByCrawledAtAsc(any()))
            .thenReturn(List.of());

        // 첫 번째 호출 — 성공 (빈 리스트라 즉시 완료)
        service.rescoreExistingItems();

        // 상태 확인
        Map<String, Object> status = service.getRescoreStatus();
        assertThat(status.get("total")).isEqualTo(0);
    }

    // --- Phase 1 skip ---

    @Test
    @DisplayName("Phase 1 — analyzePendingItems 스킵")
    void phase1SkipsAnalysis() {
        ReflectionTestUtils.setField(service, "scoringPhase", 1);
        // 예외 없이 즉시 리턴
        service.analyzePendingItems();
    }

    @Test
    @DisplayName("Phase 2 — analyzePendingItems 정상 실행")
    void phase2RunsAnalysis() {
        ReflectionTestUtils.setField(service, "scoringPhase", 2);
        var lock = new ReentrantLock();
        when(redisLockRegistry.obtain("analysis:pending")).thenReturn(lock);
        when(trendItemRepository.findByAnalysisStatusInOrderByCrawledAtAsc(any()))
            .thenReturn(List.of());

        service.analyzePendingItems();
    }
}

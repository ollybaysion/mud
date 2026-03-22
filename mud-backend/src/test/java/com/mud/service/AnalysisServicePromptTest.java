package com.mud.service;

import com.mud.domain.entity.TrendItem;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnalysisServicePromptTest {

    @Mock private WebClient claudeWebClient;
    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private CacheManager cacheManager;
    @Mock private TrendService trendService;
    @Mock private RedisLockRegistry redisLockRegistry;

    @InjectMocks private AnalysisService service;

    private TrendItem createItem(String title, String desc) {
        return TrendItem.builder()
            .title(title).originalUrl("https://example.com/" + title).urlHash("hash")
            .source(TrendItem.CrawlSource.GITHUB).description(desc)
            .koreanSummary("요약").keywords(List.of("AI", "ML"))
            .crawledAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("배치 프롬프트 — 아이템 번호 포함")
    void batchPromptContainsItemNumbers() {
        List<TrendItem> batch = List.of(createItem("Title A", "Desc A"), createItem("Title B", "Desc B"));
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildBatchPrompt", batch);
        assertThat(prompt).contains("항목 1").contains("항목 2").contains("2개의 기술 트렌드");
    }

    @Test
    @DisplayName("배치 프롬프트 — null description → '설명 없음'")
    void batchPromptNullDescription() {
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildBatchPrompt", List.of(createItem("Title", null)));
        assertThat(prompt).contains("설명 없음");
    }

    @Test
    @DisplayName("배치 프롬프트 — categorySlug에 hardware 포함")
    void batchPromptContainsHardwareCategory() {
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildBatchPrompt", List.of(createItem("Title", "desc")));
        assertThat(prompt).contains("hardware");
    }

    @Test
    @DisplayName("심층분석 프롬프트 — 필수 섹션 포함")
    void deepAnalysisPromptContainsSections() {
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildDeepAnalysisPrompt", createItem("Title", "Desc"));
        assertThat(prompt).contains("기술 개요").contains("핵심 내용")
            .contains("중요성 및 영향").contains("실무 적용 가능성").contains("관련 기술");
    }

    @Test
    @DisplayName("심층분석 프롬프트 — null 필드 fallback")
    void deepAnalysisPromptNullFields() {
        TrendItem item = TrendItem.builder().title("Title").originalUrl("https://example.com")
            .urlHash("hash").source(TrendItem.CrawlSource.GITHUB).crawledAt(LocalDateTime.now()).build();
        String prompt = ReflectionTestUtils.invokeMethod(service, "buildDeepAnalysisPrompt", item);
        assertThat(prompt).contains("설명 없음").contains("요약 없음");
    }
}

package com.mud.api.controller;

import com.mud.config.SecurityConfig;
import com.mud.scheduler.StartupCrawlRunner;
import com.mud.service.AnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"admin.api-key=test-secret", "cors.allowed-origins=http://localhost:3000"})
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StartupCrawlRunner crawlRunner;
    @MockBean private AnalysisService analysisService;
    @MockBean private CacheManager cacheManager;
    @MockBean private com.mud.service.EmailService emailService;
    @MockBean private com.mud.service.DigestService digestService;
    @MockBean private com.mud.domain.repository.DigestSubscriberRepository digestSubscriberRepository;
    @MockBean private com.mud.service.CrawlerMonitorService crawlerMonitorService;
    @MockBean private com.mud.service.ApiUsageService apiUsageService;

    @Test
    @DisplayName("POST /api/admin/crawl — 인증 없이 → 401")
    void crawlWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/admin/crawl"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/admin/crawl — 유효한 키 → 200 + 서비스 호출")
    void crawlWithAuth() throws Exception {
        mockMvc.perform(post("/api/admin/crawl")
                .header("X-API-Key", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());

        verify(crawlRunner).runAllCrawlersAsync();
    }

    @Test
    @DisplayName("POST /api/admin/analyze — 유효한 키 → 200 + 서비스 호출")
    void analyzeWithAuth() throws Exception {
        mockMvc.perform(post("/api/admin/analyze")
                .header("X-API-Key", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());

        verify(analysisService).analyzePendingItems();
    }

    @Test
    @DisplayName("POST /api/admin/flush-cache — 유효한 키 → 200 + 캐시 클리어")
    void flushCacheWithAuth() throws Exception {
        ConcurrentMapCache trendsCache = new ConcurrentMapCache("trends");
        when(cacheManager.getCacheNames()).thenReturn(List.of("trends"));
        when(cacheManager.getCache("trends")).thenReturn(trendsCache);

        mockMvc.perform(post("/api/admin/flush-cache")
                .header("X-API-Key", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());

        verify(cacheManager).getCacheNames();
        verify(cacheManager).getCache("trends");
    }

    @Test
    @DisplayName("POST /api/admin/crawl — 잘못된 키 → 401")
    void crawlWithWrongKey() throws Exception {
        mockMvc.perform(post("/api/admin/crawl")
                .header("X-API-Key", "wrong-key"))
            .andExpect(status().isUnauthorized());
    }
}

package com.mud.api.controller;

import com.mud.config.ApiKeyAuthFilter;
import com.mud.config.SecurityConfig;
import com.mud.scheduler.StartupCrawlRunner;
import com.mud.service.AnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"admin.api-key=test-secret", "cors.allowed-origins=http://localhost:3000"})
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StartupCrawlRunner crawlRunner;
    @MockBean private AnalysisService analysisService;
    @MockBean private RedisConnectionFactory redisConnectionFactory;
    @MockBean private com.mud.service.EmailService emailService;
    @MockBean private com.mud.service.DigestService digestService;
    @MockBean private com.mud.domain.repository.DigestSubscriberRepository digestSubscriberRepository;

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
    @DisplayName("POST /api/admin/flush-cache — 유효한 키 → 200")
    void flushCacheWithAuth() throws Exception {
        RedisConnection conn = mock(RedisConnection.class);
        RedisServerCommands serverCommands = mock(RedisServerCommands.class);
        when(redisConnectionFactory.getConnection()).thenReturn(conn);
        when(conn.serverCommands()).thenReturn(serverCommands);

        mockMvc.perform(post("/api/admin/flush-cache")
                .header("X-API-Key", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("POST /api/admin/crawl — 잘못된 키 → 401")
    void crawlWithWrongKey() throws Exception {
        mockMvc.perform(post("/api/admin/crawl")
                .header("X-API-Key", "wrong-key"))
            .andExpect(status().isUnauthorized());
    }
}

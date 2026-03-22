package com.mud.api.controller;

import com.mud.dto.response.CategoryResponse;
import com.mud.dto.response.TrendItemResponse;
import com.mud.dto.response.TrendPageResponse;
import com.mud.dto.response.TrendStatsResponse;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TrendController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class TrendControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TrendService trendService;
    @MockBean private AnalysisService analysisService;

    @Test
    @DisplayName("GET /api/health → 200 ok")
    void health() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    @DisplayName("GET /api/trends → 200 + 페이지 응답")
    void getTrends() throws Exception {
        TrendItemResponse item = new TrendItemResponse(
            1L, "Test", "https://example.com", "GITHUB", "desc",
            "요약", null, null, 4, List.of("AI"), null, null,
            LocalDateTime.now(), LocalDateTime.now(), null, null
        );
        TrendPageResponse page = new TrendPageResponse(List.of(item), 1, 1, 0, 20);

        when(trendService.getTrends(any())).thenReturn(page);

        mockMvc.perform(get("/api/trends"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].title").value("Test"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/trends?size=100 → size 50으로 캡핑")
    void getTrendsSizeCapped() throws Exception {
        TrendPageResponse page = new TrendPageResponse(List.of(), 0, 0, 0, 50);
        when(trendService.getTrends(any())).thenReturn(page);

        mockMvc.perform(get("/api/trends").param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @DisplayName("GET /api/trends/{id} → 200 + 상세")
    void getTrendDetail() throws Exception {
        TrendItemResponse item = new TrendItemResponse(
            1L, "Detail", "https://example.com", "GITHUB", "desc",
            "요약", "심층분석", null, 5, List.of(), null, null,
            LocalDateTime.now(), LocalDateTime.now(), null, null
        );
        when(trendService.getTrendDetail(1L)).thenReturn(item);

        mockMvc.perform(get("/api/trends/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Detail"))
            .andExpect(jsonPath("$.deepAnalysis").value("심층분석"));
    }

    @Test
    @DisplayName("GET /api/trends/{id} — 없는 아이템 → 404")
    void getTrendDetailNotFound() throws Exception {
        when(trendService.getTrendDetail(999L))
            .thenThrow(new IllegalArgumentException("Trend item not found: 999"));

        mockMvc.perform(get("/api/trends/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Trend item not found: 999"));
    }

    @Test
    @DisplayName("GET /api/categories → 200 + 카테고리 목록")
    void getCategories() throws Exception {
        List<CategoryResponse> categories = List.of(
            new CategoryResponse(1L, "ai-ml", "AI/ML", "🤖", 1),
            new CategoryResponse(2L, "java", "Java", "☕", 5)
        );
        when(trendService.getCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].slug").value("ai-ml"))
            .andExpect(jsonPath("$[1].slug").value("java"));
    }

    @Test
    @DisplayName("GET /api/stats → 200 + 통계")
    void getStats() throws Exception {
        TrendStatsResponse stats = new TrendStatsResponse(
            100, Map.of("GITHUB", 50L), Map.of("ai-ml", 40L), LocalDateTime.now()
        );
        when(trendService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(100))
            .andExpect(jsonPath("$.itemsBySource.GITHUB").value(50));
    }
}

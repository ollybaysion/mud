package com.mud.api.controller;

import com.mud.dto.request.TrendFilterRequest;
import com.mud.dto.response.CategoryResponse;
import com.mud.dto.response.TrendItemResponse;
import com.mud.dto.response.TrendPageResponse;
import com.mud.dto.response.TrendStatsResponse;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TrendController {

    private final TrendService trendService;
    private final AnalysisService analysisService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendPageResponse> getTrends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(required = false) String keyword) {

        TrendFilterRequest filter = TrendFilterRequest.builder()
            .page(page)
            .size(Math.min(size, 50))
            .categorySlug(category)
            .source(source)
            .minScore(minScore)
            .keyword(keyword)
            .build();

        return ResponseEntity.ok(trendService.getTrends(filter));
    }

    @GetMapping("/trends/{id}")
    public ResponseEntity<TrendItemResponse> getTrend(@PathVariable Long id) {
        return ResponseEntity.ok(trendService.getTrendDetail(id));
    }

    @PostMapping("/trends/{id}/deep-analysis")
    public ResponseEntity<Map<String, String>> requestDeepAnalysis(@PathVariable Long id) {
        String analysis = analysisService.generateDeepAnalysis(id);
        return ResponseEntity.ok(Map.of("deepAnalysis", analysis));
    }

    @GetMapping(value = "/trends/{id}/deep-analysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeepAnalysis(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        analysisService.generateDeepAnalysisStream(id, emitter);

        return emitter;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(trendService.getCategories());
    }

    @GetMapping("/stats")
    public ResponseEntity<TrendStatsResponse> getStats() {
        return ResponseEntity.ok(trendService.getStats());
    }
}

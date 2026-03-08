package com.mud.api.controller;

import com.mud.scheduler.StartupCrawlRunner;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final StartupCrawlRunner crawlRunner;
    private final AnalysisService analysisService;
    private final TrendService trendService;

    @PostMapping("/crawl")
    public ResponseEntity<Map<String, String>> triggerCrawl() {
        log.info("수동 크롤링 트리거");
        new Thread(crawlRunner::runAllCrawlers, "manual-crawler").start();
        return ResponseEntity.ok(Map.of("status", "크롤링 시작됨 - 백그라운드에서 실행 중"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> triggerAnalysis() {
        log.info("수동 분석 트리거");
        new Thread(() -> {
            analysisService.analyzePendingItems();
            trendService.evictTrendCaches();
        }, "manual-analyzer").start();
        return ResponseEntity.ok(Map.of("status", "분석 시작됨 - 백그라운드에서 실행 중"));
    }
}

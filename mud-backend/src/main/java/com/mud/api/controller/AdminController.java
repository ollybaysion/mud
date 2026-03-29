package com.mud.api.controller;

import com.mud.domain.repository.DigestSubscriberRepository;
import com.mud.scheduler.StartupCrawlRunner;
import com.mud.service.AnalysisService;
import com.mud.service.CrawlerMonitorService;
import com.mud.service.DigestService;
import com.mud.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final StartupCrawlRunner crawlRunner;
    private final AnalysisService analysisService;
    private final CacheManager cacheManager;
    private final CrawlerMonitorService crawlerMonitorService;
    private final EmailService emailService;
    private final DigestService digestService;
    private final DigestSubscriberRepository digestSubscriberRepository;

    @PostMapping("/crawl")
    public ResponseEntity<Map<String, String>> triggerCrawl() {
        log.info("수동 크롤링 트리거");
        crawlRunner.runAllCrawlersAsync();
        return ResponseEntity.ok(Map.of("status", "크롤링 시작됨 - 백그라운드에서 실행 중"));
    }

    @PostMapping("/flush-cache")
    public ResponseEntity<Map<String, String>> flushRedis() {
        log.info("Redis 캐시 클리어 실행");
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        return ResponseEntity.ok(Map.of("status", "Redis 캐시 삭제 완료"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> triggerAnalysis() {
        log.info("수동 분석 트리거");
        analysisService.analyzePendingItems();
        return ResponseEntity.ok(Map.of("status", "분석 시작됨 - 백그라운드에서 실행 중"));
    }

    @PostMapping("/rescore")
    public ResponseEntity<Map<String, String>> triggerRescore(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(defaultValue = "false") boolean force) {
        log.info("재평가 배치 트리거: from={}, to={}, force={}", from, to, force);
        analysisService.rescoreExistingItems(from, to, force);
        return ResponseEntity.ok(Map.of("status", "재평가 시작됨 - 백그라운드에서 실행 중"));
    }

    @GetMapping("/rescore/status")
    public ResponseEntity<Map<String, Object>> rescoreStatus() {
        return ResponseEntity.ok(analysisService.getRescoreStatus());
    }

    @GetMapping("/crawlers/status")
    public ResponseEntity<Map<String, Object>> crawlerStatus() {
        return ResponseEntity.ok(crawlerMonitorService.getCrawlerStatus());
    }

    @GetMapping("/crawlers/history")
    public ResponseEntity<Map<String, Object>> crawlerHistory(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        java.time.LocalDate actualFrom = date != null ? date : (from != null ? from : java.time.LocalDate.now());
        java.time.LocalDate actualTo = date != null ? date : (to != null ? to : java.time.LocalDate.now());
        return ResponseEntity.ok(crawlerMonitorService.getCrawlerHistory(actualFrom, actualTo, status, source));
    }

    @PostMapping("/digest/test-email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam String to) {
        log.info("테스트 이메일 발송: {}", to);
        emailService.sendHtmlEmail(to, "⚗️ Mud 테스트 이메일",
            "<div style='font-family:sans-serif;padding:20px;'><h2>⚗️ Mud</h2><p>SMTP 설정이 정상 동작합니다.</p></div>");
        return ResponseEntity.ok(Map.of("status", "테스트 이메일 발송 완료", "to", to));
    }

    @PostMapping("/digest/send-now")
    public ResponseEntity<Map<String, String>> sendDigestNow() {
        log.info("데일리 다이제스트 즉시 발송");
        digestService.sendDailyDigest();
        return ResponseEntity.ok(Map.of("status", "발송 완료"));
    }

    @GetMapping("/digest/subscribers")
    public ResponseEntity<List<Map<String, Object>>> getSubscribers() {
        List<Map<String, Object>> subscribers = digestSubscriberRepository.findAll().stream()
            .map(sub -> Map.<String, Object>of(
                "id", sub.getId(),
                "email", sub.getEmail(),
                "active", sub.isActive(),
                "subscribedAt", sub.getSubscribedAt().toString(),
                "verifiedAt", sub.getVerifiedAt() != null ? sub.getVerifiedAt().toString() : ""
            )).toList();
        return ResponseEntity.ok(subscribers);
    }
}

package com.mud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.integration.redis.util.RedisLockRegistry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final WebClient claudeWebClient;
    private final TrendItemRepository trendItemRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final CacheManager cacheManager;
    private final TrendService trendService;
    private final RedisLockRegistry redisLockRegistry;

    @Value("${claude.api.model}")
    private String claudeModel;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    @Value("${analysis.concurrency:10}")
    private int concurrency;

    @Value("${analysis.batch-size:5}")
    private int batchSize;

    @Value("${scoring.phase:2}")
    private int scoringPhase;

    @Value("${claude.api.deep-analysis-model:claude-sonnet-4-6}")
    private String deepAnalysisModel;

    @Value("${claude.api.deep-analysis-max-tokens:4096}")
    private int deepAnalysisMaxTokens;

    private Semaphore semaphore;
    private ExecutorService executor;
    private final ConcurrentHashMap<Long, CompletableFuture<String>> inFlightDeepAnalysis = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        semaphore = new Semaphore(concurrency);
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Async
    public void analyzePendingItems() {
        Lock lock = redisLockRegistry.obtain("analysis:pending");

        if (!lock.tryLock()) {
            log.info("분석이 이미 진행 중입니다 (락 보유 중), 스킵합니다");
            return;
        }

        try {
            List<TrendItem> pendingItems = trendItemRepository
                .findByAnalysisStatusInOrderByCrawledAtAsc(
                    List.of(TrendItem.AnalysisStatus.PENDING, TrendItem.AnalysisStatus.FAILED));

            if (pendingItems.isEmpty()) {
                log.debug("No pending or failed items to analyze");
                return;
            }

            List<List<TrendItem>> batches = partition(pendingItems, batchSize);
            log.info("Starting batch analysis: {} items in {} batches (batchSize={}, concurrency={})",
                pendingItems.size(), batches.size(), batchSize, concurrency);

            AtomicInteger analyzedBatches = new AtomicInteger();

            List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(
                    () -> analyzeBatch(batch, analyzedBatches, batches.size()), executor))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Analysis complete: {}/{} batches processed", analyzedBatches.get(), batches.size());
            trendService.evictTrendCaches();
        } finally {
            lock.unlock();
        }
    }

    private void analyzeBatch(List<TrendItem> batch, AtomicInteger analyzedBatches, int totalBatches) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<Long> batchIds = batch.stream().map(TrendItem::getId).toList();
        try {
            semaphore.acquire();
            try {

                tx.executeWithoutResult(status -> {
                    List<TrendItem> managed = trendItemRepository.findAllById(batchIds);
                    managed.forEach(item -> item.setAnalysisStatus(TrendItem.AnalysisStatus.PROCESSING));
                    trendItemRepository.saveAll(managed);
                });

                List<AnalysisResult> results = callClaudeApiBatch(batch);

                tx.executeWithoutResult(status -> {
                    Map<Long, TrendItem> managedMap = trendItemRepository.findAllById(batchIds)
                        .stream().collect(Collectors.toMap(TrendItem::getId, Function.identity()));
                    for (int i = 0; i < batch.size(); i++) {
                        TrendItem item = managedMap.get(batch.get(i).getId());
                        if (item == null) continue;
                        AnalysisResult result = (i < results.size()) ? results.get(i)
                            : new AnalysisResult("분석 실패", "general", 0, List.of(), 5, 0, 3, 3, null);
                        int timeliness = calculateTimeliness(item.getPublishedAt() != null ? item.getPublishedAt() : item.getCrawledAt());
                        int scoreTotal = calculateScoreTotal(result.scoringRelevance(), result.scoringActionability(), result.scoringImpact(), timeliness);
                        int stars = scoreToStars(scoreTotal);

                        item.setKoreanSummary(result.koreanSummary());
                        item.setKeywords(result.keywords());
                        item.setTopicTag(result.topicTag());
                        // 새 100점 체계
                        item.setScoreRelevance((short) result.scoringRelevance());
                        item.setScoreActionability((short) result.scoringActionability());
                        item.setScoreImpact((short) result.scoringImpact());
                        item.setScoreTimeliness((short) timeliness);
                        item.setScoreTotal((short) scoreTotal);
                        item.setRelevanceScore(stars);
                        categoryRepository.findBySlug(result.categorySlug())
                            .ifPresent(item::setCategory);
                        item.setAnalyzedAt(LocalDateTime.now());
                        item.setAnalysisStatus(TrendItem.AnalysisStatus.DONE);
                    }
                    trendItemRepository.saveAll(managedMap.values());
                });

                int count = analyzedBatches.incrementAndGet();
                log.debug("Batch complete ({}/{}): {} items", count, totalBatches, batch.size());
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Batch analysis failed: {}", e.getMessage());
            try {
                tx.executeWithoutResult(status -> {
                    List<TrendItem> managed = trendItemRepository.findAllById(batchIds);
                    managed.forEach(item -> item.setAnalysisStatus(TrendItem.AnalysisStatus.FAILED));
                    trendItemRepository.saveAll(managed);
                });
            } catch (Exception ex) {
                log.error("Failed to mark batch as FAILED: {}", ex.getMessage());
            }
        }
    }

    private List<AnalysisResult> callClaudeApiBatch(List<TrendItem> batch) {
        String prompt = buildBatchPrompt(batch);

        Map<String, Object> requestBody = Map.of(
            "model", claudeModel,
            "max_tokens", maxTokens,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        Map<?, ?> response = claudeWebClient.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(120))
            .block();

        if (response == null) throw new RuntimeException("Claude API returned null");

        String stopReason = response.get("stop_reason") != null ? response.get("stop_reason").toString() : "unknown";
        log.info("Batch analysis: batchSize={}, stopReason={}", batch.size(), stopReason);
        if ("max_tokens".equals(stopReason)) {
            log.warn("Batch analysis truncated by max_tokens: batchSize={}", batch.size());
        }

        String text = extractResponseText(response);
        List<AnalysisResult> results = parseBatchResult(text, batch.size());
        if (results.isEmpty()) {
            throw new RuntimeException("Claude 응답 파싱 실패: 결과가 비어있습니다");
        }
        return results;
    }

    private String buildBatchPrompt(List<TrendItem> batch) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            TrendItem item = batch.get(i);
            String desc = item.getDescription() != null ? item.getDescription() : "설명 없음";
            items.append("항목 %d:\n제목: %s\n출처: %s\n설명: %s\n\n"
                .formatted(i + 1, item.getTitle(), item.getSource().name(), desc));
        }

        return """
            다음 %d개의 기술 트렌드 항목을 분석하고, 반드시 아래 JSON 배열 형식으로만 응답하세요.
            JSON 외의 다른 텍스트, 마크다운 코드블록은 포함하지 마세요.
            항목 순서대로 결과를 배열에 담아주세요.

            %s
            응답 JSON 형식:
            [
              {
                "koreanSummary": "현업 개발자 관점에서 2-3문장으로 핵심을 요약 (한국어로 작성)",
                "categorySlug": "ai-ml 또는 rag 또는 llm 또는 cpp 또는 java 또는 devops 또는 webdev 또는 security 또는 hardware 또는 general 중 하나",
                "keywords": ["키워드1", "키워드2", "키워드3"],
                "scoring": {
                  "relevance": 0부터 10 사이의 정수,
                  "actionability": 0부터 10 사이의 정수,
                  "impact": 0부터 10 사이의 정수
                },
                "topicTag": "주제를 대표하는 영문 소문자 태그 (예: kubernetes, react, rust, llm)"
              }
            ]

            scoring 채점 기준:

            기술 관련성 (relevance, 0~10):
            10 = 코드/프레임워크/도구 직접 다룸 (신규 릴리즈, CVE 패치)
             7 = 아키텍처, 설계 패턴, 성능 최적화
             5 = 기술 업계 동향, 개발 문화
             3 = 기술과 간접 관련 (기업 인사, 투자)
             0 = 기술과 무관 (정치, 스포츠, 요리)

            실용성 (actionability, 0~10):
            10 = 코드 복사해서 바로 적용 가능
             7 = 실무 의사결정에 직접 참고
             5 = 배경 지식으로 유용
             3 = 인사이트는 있지만 행동 어려움
             0 = 실무 적용 불가

            임팩트 (impact, 0~10):
            10 = 대다수 개발자에게 즉시 영향
             7 = 특정 생태계 전체에 영향
             5 = 해당 기술 스택 사용자에게 영향
             3 = 니치하지만 깊은 영향
             0 = 영향 범위 극소

            주의: GitHub Trending의 기존 유명 프로젝트(이미 널리 알려진 라이브러리/프레임워크)는
            새로운 릴리즈, 메이저 업데이트, 보안 패치 등 구체적 변경이 없는 한
            relevance와 actionability를 5 이하로 평가하세요.
            """.formatted(batch.size(), items);
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(Map<?, ?> response) {
        List<?> content = (List<?>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Empty Claude response content");
        }
        Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
        return (String) firstBlock.get("text");
    }

    private List<AnalysisResult> parseBatchResult(String text, int expectedSize) {
        try {
            String json = text.replaceAll("(?s)^\\s*```(?:json)?\\s*|\\s*```\\s*$", "").trim();

            JsonNode arrayNode = objectMapper.readTree(json);
            if (!arrayNode.isArray()) {
                log.warn("Expected JSON array but got: {}", json);
                return List.of();
            }

            List<AnalysisResult> results = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                String koreanSummary = node.path("koreanSummary").asText("요약 없음");
                String categorySlug = node.path("categorySlug").asText("general");
                List<String> keywords = new ArrayList<>();
                JsonNode kwNode = node.path("keywords");
                if (kwNode.isArray()) {
                    kwNode.forEach(kw -> keywords.add(kw.asText()));
                }

                JsonNode scoring = node.path("scoring");
                int sRelevance = Math.max(0, Math.min(10, scoring.path("relevance").asInt(5)));
                int sActionability = Math.max(0, Math.min(10, scoring.path("actionability").asInt(3)));
                int sImpact = Math.max(0, Math.min(10, scoring.path("impact").asInt(3)));

                String topicTag = node.path("topicTag").asText(null);

                // relevanceScore는 저장 시 timeliness 포함하여 계산
                results.add(new AnalysisResult(koreanSummary, categorySlug, 0, keywords,
                    sRelevance, 0, sActionability, sImpact, topicTag));
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse batch Claude response: {}. Text was: {}", e.getMessage(), text);
            return List.of();
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    int calculateTimeliness(LocalDateTime publishedAt) {
        if (publishedAt == null) return 0;
        long hoursAgo = java.time.Duration.between(publishedAt, LocalDateTime.now()).toHours();
        if (hoursAgo <= 6) return 10;
        if (hoursAgo <= 24) return 8;
        if (hoursAgo <= 72) return 6;   // 3일
        if (hoursAgo <= 168) return 4;  // 7일
        if (hoursAgo <= 336) return 2;  // 14일
        return 0;
    }

    int calculateScoreTotal(int relevance, int actionability, int impact, int timeliness) {
        double score = relevance * 3.5 + actionability * 3.0 + impact * 1.5 + timeliness * 2.0;
        return (int) Math.round(Math.min(100, Math.max(0, score)));
    }

    int scoreToStars(int scoreTotal) {
        if (scoreTotal < 15) return 1;
        if (scoreTotal < 25) return 1;  // ★1.5 → 1로 매핑 (정수)
        if (scoreTotal < 35) return 2;
        if (scoreTotal < 45) return 2;  // ★2.5 → 2로 매핑
        if (scoreTotal < 55) return 3;
        if (scoreTotal < 65) return 3;  // ★3.5 → 3으로 매핑
        if (scoreTotal < 75) return 4;
        if (scoreTotal < 85) return 4;  // ★4.5 → 4로 매핑
        return 5;
    }

    record AnalysisResult(
        String koreanSummary,
        String categorySlug,
        int relevanceScore,
        List<String> keywords,
        int scoringRelevance,
        int scoringTimeliness,
        int scoringActionability,
        int scoringImpact,
        String topicTag
    ) {}

    // --- Rescore ---

    private final AtomicInteger rescoreProcessed = new AtomicInteger(0);
    private final AtomicInteger rescoreFailed = new AtomicInteger(0);
    private volatile int rescoreTotal = 0;
    private final java.util.concurrent.atomic.AtomicBoolean rescoreInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Async
    public void rescoreExistingItems(java.time.LocalDate from, java.time.LocalDate to, boolean force) {
        Lock rescoreLock = redisLockRegistry.obtain("analysis:rescore");
        if (!rescoreLock.tryLock()) {
            log.info("재평가가 이미 진행 중입니다 (분산 락 보유 중)");
            return;
        }

        if (!rescoreInProgress.compareAndSet(false, true)) {
            rescoreLock.unlock();
            log.info("재평가가 이미 진행 중입니다");
            return;
        }
        rescoreProcessed.set(0);
        rescoreFailed.set(0);

        try {
            TransactionTemplate txRead = new TransactionTemplate(transactionManager);
            txRead.setReadOnly(true);
            List<Long> doneItemIds = txRead.execute(status -> {
                if (from != null && force) {
                    LocalDateTime fromDt = from.atStartOfDay();
                    LocalDateTime toDt = (to != null ? to : java.time.LocalDate.now()).plusDays(1).atStartOfDay();
                    return trendItemRepository.findByStatusAndPeriodWithCategory(
                        TrendItem.AnalysisStatus.DONE, fromDt, toDt)
                        .stream().map(TrendItem::getId).toList();
                }
                if (from != null) {
                    LocalDateTime fromDt = from.atStartOfDay();
                    LocalDateTime toDt = (to != null ? to : java.time.LocalDate.now()).plusDays(1).atStartOfDay();
                    return trendItemRepository.findByAnalysisStatusAndCrawledAtBetweenAndScoreTotalIsNull(
                        TrendItem.AnalysisStatus.DONE, fromDt, toDt)
                        .stream().map(TrendItem::getId).toList();
                }
                if (force) {
                    return trendItemRepository.findByAnalysisStatusOrderByCrawledAtAsc(TrendItem.AnalysisStatus.DONE)
                        .stream().map(TrendItem::getId).toList();
                }
                return trendItemRepository.findByAnalysisStatusAndScoreTotalIsNullOrderByCrawledAtAsc(TrendItem.AnalysisStatus.DONE)
                    .stream().map(TrendItem::getId).toList();
            });

            rescoreTotal = doneItemIds.size();
            log.info("재평가 시작: {}개 아이템", rescoreTotal);

            List<List<Long>> batches = new ArrayList<>();
            for (int i = 0; i < doneItemIds.size(); i += batchSize) {
                batches.add(doneItemIds.subList(i, Math.min(i + batchSize, doneItemIds.size())));
            }

            for (List<Long> batchIds : batches) {
                try {
                    semaphore.acquire();
                    try {
                        TransactionTemplate tx = new TransactionTemplate(transactionManager);
                        List<TrendItem> items = tx.execute(status -> {
                            List<TrendItem> managed = trendItemRepository.findAllById(batchIds);
                            managed.forEach(item -> item.getKeywords().size());
                            return managed;
                        });

                        List<AnalysisResult> results = callClaudeApiBatch(items);

                        tx.executeWithoutResult(status -> {
                            Map<Long, TrendItem> managedMap = trendItemRepository.findAllById(batchIds)
                                .stream().collect(Collectors.toMap(TrendItem::getId, Function.identity()));
                            for (int i = 0; i < items.size(); i++) {
                                TrendItem item = managedMap.get(items.get(i).getId());
                                if (item == null) continue;
                                AnalysisResult result = (i < results.size()) ? results.get(i)
                                    : new AnalysisResult("분석 실패", "general", 0, List.of(), 5, 0, 3, 3, null);
                                int timeliness = calculateTimeliness(item.getPublishedAt() != null ? item.getPublishedAt() : item.getCrawledAt());
                                int scoreTotal = calculateScoreTotal(result.scoringRelevance(), result.scoringActionability(), result.scoringImpact(), timeliness);
                                int stars = scoreToStars(scoreTotal);

                                item.setKoreanSummary(result.koreanSummary());
                                item.setKeywords(result.keywords());
                                item.setTopicTag(result.topicTag());
                                item.setScoreRelevance((short) result.scoringRelevance());
                                item.setScoreActionability((short) result.scoringActionability());
                                item.setScoreImpact((short) result.scoringImpact());
                                item.setScoreTimeliness((short) timeliness);
                                item.setScoreTotal((short) scoreTotal);
                                item.setRelevanceScore(stars);
                                categoryRepository.findBySlug(result.categorySlug())
                                    .ifPresent(item::setCategory);
                                item.setAnalyzedAt(LocalDateTime.now());
                                if (scoreTotal < 15) {
                                    item.setAnalysisStatus(TrendItem.AnalysisStatus.REJECTED);
                                }
                            }
                            trendItemRepository.saveAll(managedMap.values());
                        });

                        rescoreProcessed.addAndGet(batchIds.size());
                        log.info("재평가 진행: {}/{}", rescoreProcessed.get(), rescoreTotal);
                    } finally {
                        semaphore.release();
                    }

                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("재평가 배치 실패: {}", e.getMessage());
                    rescoreFailed.addAndGet(batchIds.size());
                }
            }

            trendService.evictTrendCaches();
            log.info("재평가 완료: {}/{}", rescoreProcessed.get(), rescoreTotal);
        } finally {
            rescoreInProgress.set(false);
            rescoreLock.unlock();
        }
    }

    public Map<String, Object> getRescoreStatus() {
        return Map.of(
            "inProgress", rescoreInProgress.get(),
            "processed", rescoreProcessed.get(),
            "failed", rescoreFailed.get(),
            "total", rescoreTotal
        );
    }

    public String generateDeepAnalysis(Long trendItemId) {
        TransactionTemplate txRead = new TransactionTemplate(transactionManager);
        txRead.setReadOnly(true);
        TrendItem item = txRead.execute(status -> {
            TrendItem i = trendItemRepository.findById(trendItemId)
                .orElseThrow(() -> new IllegalArgumentException("Trend item not found: " + trendItemId));
            // 비동기 스레드에서 Hibernate 세션이 없으므로 lazy 컬렉션을 미리 초기화
            i.getKeywords().size();
            return i;
        });

        if (item.getDeepAnalysis() != null) {
            return item.getDeepAnalysis();
        }

        CompletableFuture<String> future = inFlightDeepAnalysis.computeIfAbsent(trendItemId, id -> {
            CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeDeepAnalysis(item);
                } finally {
                    inFlightDeepAnalysis.remove(id);
                }
            }, executor);
            return f;
        });

        return future.join();
    }

    public void generateDeepAnalysisStream(Long trendItemId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("progress")
                .data(Map.of("stage", "started", "percent", 0), MediaType.APPLICATION_JSON));

            TransactionTemplate txRead = new TransactionTemplate(transactionManager);
            txRead.setReadOnly(true);
            TrendItem item = txRead.execute(status -> {
                TrendItem i = trendItemRepository.findById(trendItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Trend item not found: " + trendItemId));
                i.getKeywords().size();
                return i;
            });

            if (item.getDeepAnalysis() != null) {
                emitter.send(SseEmitter.event().name("result").data(item.getDeepAnalysis()));
                emitter.complete();
                return;
            }

            emitter.send(SseEmitter.event().name("progress")
                .data(Map.of("stage", "analyzing", "percent", 30), MediaType.APPLICATION_JSON));

            CompletableFuture<String> future = inFlightDeepAnalysis.computeIfAbsent(trendItemId, id ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeDeepAnalysis(item);
                    } finally {
                        inFlightDeepAnalysis.remove(id);
                    }
                }, executor)
            );

            future.thenAccept(analysis -> {
                try {
                    emitter.send(SseEmitter.event().name("progress")
                        .data(Map.of("stage", "done", "percent", 100), MediaType.APPLICATION_JSON));
                    emitter.send(SseEmitter.event().name("result").data(analysis));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }).exceptionally(ex -> {
                try {
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("code", "ANALYSIS_FAILED", "message", errorMsg), MediaType.APPLICATION_JSON));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Deep analysis stream failed for item {}: {}", trendItemId, e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private String executeDeepAnalysis(TrendItem item) {
        String prompt = buildDeepAnalysisPrompt(item);

        Map<String, Object> requestBody = Map.of(
            "model", deepAnalysisModel,
            "max_tokens", deepAnalysisMaxTokens,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        Map<?, ?> response = claudeWebClient.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(240))
            .block();

        if (response == null) throw new RuntimeException("Claude API returned null for deep analysis");

        String stopReason = response.get("stop_reason") != null ? response.get("stop_reason").toString() : "unknown";
        log.info("Deep analysis: itemId={}, stopReason={}", item.getId(), stopReason);
        if ("max_tokens".equals(stopReason)) {
            log.warn("Deep analysis truncated by max_tokens: itemId={}", item.getId());
        }

        String analysis = extractResponseText(response);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            TrendItem managed = trendItemRepository.findById(item.getId()).orElseThrow();
            managed.setDeepAnalysis(analysis);
            trendItemRepository.save(managed);
        });

        var cache = cacheManager.getCache("trend-detail");
        if (cache != null) {
            cache.evict(item.getId());
        }

        log.info("Deep analysis completed for item {}: {}", item.getId(), item.getTitle());
        return analysis;
    }

    private String buildDeepAnalysisPrompt(TrendItem item) {
        String desc = item.getDescription() != null ? item.getDescription() : "설명 없음";
        String summary = item.getKoreanSummary() != null ? item.getKoreanSummary() : "요약 없음";
        String keywords = item.getKeywords() != null ? String.join(", ", item.getKeywords()) : "";

        return """
            다음 기술 트렌드 항목에 대해 심층 분석을 작성해주세요.
            반드시 한국어로 작성하고, 마크다운 형식을 사용하세요.

            제목: %s
            출처: %s
            원문 URL: %s
            기존 요약: %s
            설명: %s
            키워드: %s

            아래 섹션 구조로 상세하게 분석해주세요:

            ## 기술 개요
            이 기술/프로젝트/논문이 무엇인지 배경과 함께 상세히 설명

            ## 핵심 내용
            주요 특징, 기능, 기여점을 bullet point로 정리

            ## 중요성 및 영향
            이 기술이 업계에 미치는 영향과 왜 중요한지 설명

            ## 실무 적용 가능성
            현업 개발자가 실제로 어떻게 활용할 수 있는지 구체적 시나리오 제시

            ## 관련 기술
            함께 알아두면 좋은 관련 기술, 대안, 경쟁 기술 소개

            마크다운 형식으로 깔끔하게 작성하되, 코드블록(```)은 필요한 경우에만 사용하세요.
            각 섹션은 충분히 상세하게 작성해주세요.
            """.formatted(item.getTitle(), item.getSource().name(), item.getOriginalUrl(),
                summary, desc, keywords);
    }
}

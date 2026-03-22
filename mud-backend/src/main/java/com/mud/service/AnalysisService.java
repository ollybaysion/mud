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
        try {
            semaphore.acquire();
            try {
                tx.executeWithoutResult(status -> {
                    for (TrendItem item : batch) {
                        TrendItem managed = trendItemRepository.findById(item.getId()).orElseThrow();
                        managed.setAnalysisStatus(TrendItem.AnalysisStatus.PROCESSING);
                        trendItemRepository.save(managed);
                    }
                });

                List<AnalysisResult> results = callClaudeApiBatch(batch);

                tx.executeWithoutResult(status -> {
                    for (int i = 0; i < batch.size(); i++) {
                        TrendItem managed = trendItemRepository.findById(batch.get(i).getId()).orElseThrow();
                        AnalysisResult result = (i < results.size()) ? results.get(i)
                            : new AnalysisResult("분석 실패", "general", 3, List.of());
                        managed.setKoreanSummary(result.koreanSummary());
                        managed.setRelevanceScore(result.relevanceScore());
                        managed.setKeywords(result.keywords());
                        categoryRepository.findBySlug(result.categorySlug())
                            .ifPresent(managed::setCategory);
                        managed.setAnalyzedAt(LocalDateTime.now());
                        managed.setAnalysisStatus(TrendItem.AnalysisStatus.DONE);
                        trendItemRepository.save(managed);
                    }
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
                    for (TrendItem item : batch) {
                        TrendItem managed = trendItemRepository.findById(item.getId()).orElseThrow();
                        managed.setAnalysisStatus(TrendItem.AnalysisStatus.FAILED);
                        trendItemRepository.save(managed);
                    }
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

        String text = extractResponseText(response);
        return parseBatchResult(text, batch.size());
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
                "categorySlug": "ai-ml 또는 rag 또는 llm 또는 cpp 또는 java 또는 devops 또는 webdev 또는 security 또는 general 중 하나",
                "relevanceScore": 1부터 5 사이의 정수,
                "keywords": ["키워드1", "키워드2", "키워드3"]
              }
            ]

            relevanceScore 기준:
            5 = 즉시 실무에 적용 가능한 핵심 기술
            4 = 알아두면 유용한 중요 트렌드
            3 = 관심 분야라면 볼만한 내용
            2 = 참고 수준의 정보
            1 = 현업 개발자 관련성 낮음
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
            String json = text.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

            JsonNode arrayNode = objectMapper.readTree(json);
            if (!arrayNode.isArray()) {
                log.warn("Expected JSON array but got: {}", json);
                return List.of();
            }

            List<AnalysisResult> results = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                String koreanSummary = node.path("koreanSummary").asText("요약 없음");
                String categorySlug = node.path("categorySlug").asText("general");
                int relevanceScore = Math.max(1, Math.min(5, node.path("relevanceScore").asInt(3)));
                List<String> keywords = new ArrayList<>();
                JsonNode kwNode = node.path("keywords");
                if (kwNode.isArray()) {
                    kwNode.forEach(kw -> keywords.add(kw.asText()));
                }
                results.add(new AnalysisResult(koreanSummary, categorySlug, relevanceScore, keywords));
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

    record AnalysisResult(
        String koreanSummary,
        String categorySlug,
        int relevanceScore,
        List<String> keywords
    ) {}

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
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("progress").data("{\"stage\":\"started\",\"percent\":0}"));

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

                emitter.send(SseEmitter.event().name("progress").data("{\"stage\":\"analyzing\",\"percent\":30}"));

                String analysis = executeDeepAnalysis(item);

                emitter.send(SseEmitter.event().name("progress").data("{\"stage\":\"done\",\"percent\":100}"));
                emitter.send(SseEmitter.event().name("result").data(analysis));
                emitter.complete();
            } catch (Exception e) {
                log.error("Deep analysis stream failed for item {}: {}", trendItemId, e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("{\"code\":\"ANALYSIS_FAILED\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, executor);
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

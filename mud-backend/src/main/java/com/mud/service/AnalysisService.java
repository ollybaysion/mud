package com.mud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final WebClient claudeWebClient;
    private final TrendItemRepository trendItemRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.model}")
    private String claudeModel;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    @Async
    @Transactional
    public void analyzePendingItems() {
        List<TrendItem> pendingItems = trendItemRepository
            .findByAnalysisStatusOrderByCrawledAtAsc(TrendItem.AnalysisStatus.PENDING);

        if (pendingItems.isEmpty()) {
            log.debug("No pending items to analyze");
            return;
        }

        log.info("Starting analysis of {} pending items", pendingItems.size());
        int analyzed = 0;

        for (TrendItem item : pendingItems) {
            try {
                item.setAnalysisStatus(TrendItem.AnalysisStatus.PROCESSING);
                trendItemRepository.save(item);

                AnalysisResult result = callClaudeApi(item);

                item.setKoreanSummary(result.koreanSummary());
                item.setRelevanceScore(result.relevanceScore());
                item.setKeywords(result.keywords());
                categoryRepository.findBySlug(result.categorySlug())
                    .ifPresent(item::setCategory);
                item.setAnalyzedAt(LocalDateTime.now());
                item.setAnalysisStatus(TrendItem.AnalysisStatus.DONE);
                trendItemRepository.save(item);

                analyzed++;
                log.debug("Analysis complete ({}/{}): {}", analyzed, pendingItems.size(), item.getTitle());
                Thread.sleep(600);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Analysis interrupted after {}/{} items", analyzed, pendingItems.size());
                return;
            } catch (Exception e) {
                log.error("Analysis failed for item id={}, title={}: {}",
                    item.getId(), item.getTitle(), e.getMessage());
                item.setAnalysisStatus(TrendItem.AnalysisStatus.FAILED);
                trendItemRepository.save(item);
            }
        }

        log.info("Analysis complete: {}/{} items analyzed", analyzed, pendingItems.size());
    }

    private AnalysisResult callClaudeApi(TrendItem item) {
        String prompt = buildPrompt(item);

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
            .timeout(Duration.ofSeconds(60))
            .block();

        if (response == null) throw new RuntimeException("Claude API returned null");

        String text = extractResponseText(response);
        return parseAnalysisResult(text);
    }

    private String buildPrompt(TrendItem item) {
        String desc = item.getDescription() != null ? item.getDescription() : "설명 없음";
        return """
            다음 기술 트렌드 항목을 분석하고, 반드시 아래 JSON 형식으로만 응답하세요.
            JSON 외의 다른 텍스트, 마크다운 코드블록은 포함하지 마세요.

            제목: %s
            출처: %s
            설명: %s

            응답 JSON 형식:
            {
              "koreanSummary": "현업 개발자 관점에서 2-3문장으로 핵심을 요약 (한국어로 작성)",
              "categorySlug": "ai-ml 또는 rag 또는 llm 또는 cpp 또는 java 또는 devops 또는 webdev 또는 security 또는 general 중 하나",
              "relevanceScore": 1부터 5 사이의 정수,
              "keywords": ["키워드1", "키워드2", "키워드3"]
            }

            relevanceScore 기준:
            5 = 즉시 실무에 적용 가능한 핵심 기술
            4 = 알아두면 유용한 중요 트렌드
            3 = 관심 분야라면 볼만한 내용
            2 = 참고 수준의 정보
            1 = 현업 개발자 관련성 낮음
            """.formatted(item.getTitle(), item.getSource().name(), desc);
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

    private AnalysisResult parseAnalysisResult(String text) {
        try {
            // Strip any accidental markdown code fences
            String json = text.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

            JsonNode node = objectMapper.readTree(json);

            String koreanSummary = node.path("koreanSummary").asText("요약 없음");
            String categorySlug = node.path("categorySlug").asText("general");
            int relevanceScore = node.path("relevanceScore").asInt(3);
            relevanceScore = Math.max(1, Math.min(5, relevanceScore));

            List<String> keywords = new ArrayList<>();
            JsonNode kwNode = node.path("keywords");
            if (kwNode.isArray()) {
                kwNode.forEach(kw -> keywords.add(kw.asText()));
            }

            return new AnalysisResult(koreanSummary, categorySlug, relevanceScore, keywords);
        } catch (Exception e) {
            log.warn("Failed to parse Claude response: {}. Text was: {}", e.getMessage(), text);
            return new AnalysisResult("분석 실패", "general", 3, List.of());
        }
    }

    record AnalysisResult(
        String koreanSummary,
        String categorySlug,
        int relevanceScore,
        List<String> keywords
    ) {}
}

package com.mud.service;

import com.mud.domain.entity.ApiUsageLog;
import com.mud.domain.entity.ApiUsageLog.ApiType;
import com.mud.domain.repository.ApiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiUsageService {

    private final ApiUsageLogRepository apiUsageLogRepository;

    // 모델별 토큰 단가 (per 1M tokens, USD)
    private static final Map<String, double[]> PRICING = Map.of(
        "claude-haiku-4-5-20251001", new double[]{0.25, 1.25},
        "claude-sonnet-4-6",         new double[]{3.00, 15.00}
    );

    /**
     * Claude API 응답의 usage 필드에서 토큰 정보를 파싱하여 비동기 기록
     */
    @Async
    public void logUsage(ApiType apiType, String model, Map<?, ?> response) {
        try {
            Map<?, ?> usage = (Map<?, ?>) response.get("usage");
            if (usage == null) {
                log.debug("No usage field in Claude API response for {}", apiType);
                return;
            }

            int inputTokens = ((Number) usage.get("input_tokens")).intValue();
            int outputTokens = ((Number) usage.get("output_tokens")).intValue();
            BigDecimal cost = calculateCost(model, inputTokens, outputTokens);

            ApiUsageLog logEntry = ApiUsageLog.builder()
                .apiType(apiType)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .estimatedCost(cost)
                .calledAt(LocalDateTime.now())
                .build();

            apiUsageLogRepository.save(logEntry);
            log.debug("API usage logged: type={}, model={}, in={}, out={}, cost=${}",
                apiType, model, inputTokens, outputTokens, cost);
        } catch (Exception e) {
            log.warn("Failed to log API usage: {}", e.getMessage());
        }
    }

    public Map<String, Object> getSummary(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        LocalDate periodStart = LocalDate.now().minusDays(days);

        List<Object[]> byType = apiUsageLogRepository.summarizeByType(since);
        List<Object[]> daily = apiUsageLogRepository.dailyTrend(since);

        long totalCalls = 0;
        long totalInput = 0;
        long totalOutput = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, Map<String, Object>> byTypeMap = new LinkedHashMap<>();

        for (Object[] row : byType) {
            ApiType type = (ApiType) row[0];
            long calls = (Long) row[1];
            long inTok = (Long) row[2];
            long outTok = (Long) row[3];
            BigDecimal cost = (BigDecimal) row[4];

            totalCalls += calls;
            totalInput += inTok;
            totalOutput += outTok;
            totalCost = totalCost.add(cost);

            byTypeMap.put(type.name(), Map.of(
                "calls", calls,
                "inputTokens", inTok,
                "outputTokens", outTok,
                "cost", "$" + cost.setScale(3, RoundingMode.HALF_UP)
            ));
        }

        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (Object[] row : daily) {
            LocalDate date = (LocalDate) row[0];
            dailyTrend.add(Map.of(
                "date", date.toString(),
                "calls", (Long) row[1],
                "cost", "$" + ((BigDecimal) row[2 + 2]).setScale(3, RoundingMode.HALF_UP)
            ));
        }

        return Map.of(
            "period", periodStart + " ~ " + LocalDate.now(),
            "totalCalls", totalCalls,
            "totalInputTokens", totalInput,
            "totalOutputTokens", totalOutput,
            "estimatedCost", "$" + totalCost.setScale(3, RoundingMode.HALF_UP),
            "byType", byTypeMap,
            "dailyTrend", dailyTrend
        );
    }

    private BigDecimal calculateCost(String model, int inputTokens, int outputTokens) {
        double[] rates = PRICING.getOrDefault(model, new double[]{3.00, 15.00});
        double inputCost = inputTokens * rates[0] / 1_000_000;
        double outputCost = outputTokens * rates[1] / 1_000_000;
        return BigDecimal.valueOf(inputCost + outputCost).setScale(6, RoundingMode.HALF_UP);
    }
}

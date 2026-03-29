package com.mud.service;

import com.mud.domain.entity.ApiUsageLog;
import com.mud.domain.entity.ApiUsageLog.ApiType;
import com.mud.domain.repository.ApiUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiUsageServiceTest {

    @Mock
    private ApiUsageLogRepository apiUsageLogRepository;

    @InjectMocks
    private ApiUsageService apiUsageService;

    @Test
    @DisplayName("logUsage — usage 필드가 있으면 정상 기록")
    void logUsageSuccess() {
        Map<String, Object> response = Map.of(
            "usage", Map.of("input_tokens", 1500, "output_tokens", 800)
        );

        apiUsageService.logUsage(ApiType.BATCH_ANALYSIS, "claude-haiku-4-5-20251001", response);

        ArgumentCaptor<ApiUsageLog> captor = ArgumentCaptor.forClass(ApiUsageLog.class);
        verify(apiUsageLogRepository).save(captor.capture());

        ApiUsageLog saved = captor.getValue();
        assertThat(saved.getApiType()).isEqualTo(ApiType.BATCH_ANALYSIS);
        assertThat(saved.getModel()).isEqualTo("claude-haiku-4-5-20251001");
        assertThat(saved.getInputTokens()).isEqualTo(1500);
        assertThat(saved.getOutputTokens()).isEqualTo(800);
        // Haiku: 1500 * 0.25/1M + 800 * 1.25/1M = 0.000375 + 0.001 = 0.001375
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("0.001375"));
    }

    @Test
    @DisplayName("logUsage — usage 필드 없으면 기록하지 않음")
    void logUsageNoUsageField() {
        Map<String, Object> response = Map.of("content", "test");

        apiUsageService.logUsage(ApiType.DEEP_ANALYSIS, "claude-sonnet-4-6", response);

        verify(apiUsageLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logUsage — Sonnet 모델 비용 계산")
    void logUsageSonnetCost() {
        Map<String, Object> response = Map.of(
            "usage", Map.of("input_tokens", 1000000, "output_tokens", 500)
        );

        apiUsageService.logUsage(ApiType.DEEP_ANALYSIS, "claude-sonnet-4-6", response);

        ArgumentCaptor<ApiUsageLog> captor = ArgumentCaptor.forClass(ApiUsageLog.class);
        verify(apiUsageLogRepository).save(captor.capture());

        ApiUsageLog saved = captor.getValue();
        // Sonnet: 1M * 3.00/1M + 500 * 15.00/1M = 3.0 + 0.0075 = 3.0075
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("3.007500"));
    }
}

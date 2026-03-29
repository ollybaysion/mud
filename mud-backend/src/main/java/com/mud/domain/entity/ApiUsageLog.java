package com.mud.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_usage_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_type", nullable = false, length = 30)
    private ApiType apiType;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost", nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    public enum ApiType {
        BATCH_ANALYSIS,
        DEEP_ANALYSIS,
        WEEKLY_REPORT,
        RESCORE
    }
}

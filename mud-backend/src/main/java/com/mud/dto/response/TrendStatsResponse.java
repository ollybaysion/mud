package com.mud.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record TrendStatsResponse(
    long totalItems,
    Map<String, Long> itemsBySource,
    Map<String, Long> itemsByCategory,
    LocalDateTime generatedAt
) {}

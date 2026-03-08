package com.mud.dto.response;

import com.mud.domain.entity.TrendItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record TrendItemResponse(
    Long id,
    String title,
    String originalUrl,
    String source,
    String description,
    String koreanSummary,
    CategoryResponse category,
    Integer relevanceScore,
    List<String> keywords,
    LocalDateTime publishedAt,
    LocalDateTime crawledAt,
    Integer githubStars,
    String githubLanguage
) {
    public static TrendItemResponse from(TrendItem item) {
        return new TrendItemResponse(
            item.getId(),
            item.getTitle(),
            item.getOriginalUrl(),
            item.getSource().name(),
            item.getDescription(),
            item.getKoreanSummary(),
            item.getCategory() != null ? CategoryResponse.from(item.getCategory()) : null,
            item.getRelevanceScore(),
            new ArrayList<>(item.getKeywords()),
            item.getPublishedAt(),
            item.getCrawledAt(),
            item.getGithubStars(),
            item.getGithubLanguage()
        );
    }
}

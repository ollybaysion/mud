package com.mud.dto.response;

import com.mud.domain.entity.TrendItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record TrendItemResponse(
    Long id,
    String title,
    String originalUrl,
    String source,
    String description,
    String koreanSummary,
    String deepAnalysis,
    CategoryResponse category,
    Integer relevanceScore,
    Short scoreTotal,
    List<String> keywords,
    Map<String, Short> scoring,
    String topicTag,
    LocalDateTime publishedAt,
    LocalDateTime crawledAt,
    Integer githubStars,
    String githubLanguage
) {
    public static TrendItemResponse from(TrendItem item) {
        Map<String, Short> scoring = null;
        if (item.getScoreRelevance() != null && item.getScoreTimeliness() != null
                && item.getScoreActionability() != null && item.getScoreImpact() != null) {
            scoring = Map.of(
                "relevance", item.getScoreRelevance(),
                "timeliness", item.getScoreTimeliness(),
                "actionability", item.getScoreActionability(),
                "impact", item.getScoreImpact()
            );
        }

        return new TrendItemResponse(
            item.getId(),
            item.getTitle(),
            item.getOriginalUrl(),
            item.getSource().name(),
            item.getDescription(),
            item.getKoreanSummary(),
            item.getDeepAnalysis(),
            item.getCategory() != null ? CategoryResponse.from(item.getCategory()) : null,
            item.getRelevanceScore(),
            item.getScoreTotal(),
            new ArrayList<>(item.getKeywords()),
            scoring,
            item.getTopicTag(),
            item.getPublishedAt(),
            item.getCrawledAt(),
            item.getGithubStars(),
            item.getGithubLanguage()
        );
    }
}

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
    List<String> keywords,
    Map<String, Integer> scoring,
    String topicTag,
    LocalDateTime publishedAt,
    LocalDateTime crawledAt,
    Integer githubStars,
    String githubLanguage
) {
    public static TrendItemResponse from(TrendItem item) {
        Map<String, Integer> scoring = null;
        if (item.getScoringRelevance() != null) {
            scoring = Map.of(
                "relevance", item.getScoringRelevance(),
                "timeliness", item.getScoringTimeliness(),
                "actionability", item.getScoringActionability(),
                "impact", item.getScoringImpact()
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

package com.mud.dto.response;

import com.mud.domain.entity.Category;

public record CategoryResponse(
    Long id,
    String slug,
    String displayName,
    String emoji,
    Integer sortOrder
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getSlug(),
            category.getDisplayName(),
            category.getEmoji(),
            category.getSortOrder()
        );
    }
}

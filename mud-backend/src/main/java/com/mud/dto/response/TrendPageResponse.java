package com.mud.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record TrendPageResponse(
    List<TrendItemResponse> content,
    long totalElements,
    int totalPages,
    int number,
    int size
) {
    public static TrendPageResponse from(Page<TrendItemResponse> page) {
        return new TrendPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}

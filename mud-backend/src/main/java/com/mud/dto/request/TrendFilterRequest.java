package com.mud.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrendFilterRequest {

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private String categorySlug;
    private String source;

    @Builder.Default
    private int minScore = 1;

    private String keyword;

    public String cacheKey() {
        return String.format("p%d_s%d_c%s_src%s_ms%d_kw%s",
            page, size,
            categorySlug != null ? categorySlug : "all",
            source != null ? source : "all",
            minScore,
            keyword != null ? keyword : ""
        );
    }
}

package com.mud.dto;

import com.mud.dto.request.TrendFilterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrendFilterRequestTest {

    @Test
    @DisplayName("전체 필터 캐시 키 생성")
    void cacheKeyWithAllFilters() {
        TrendFilterRequest filter = TrendFilterRequest.builder()
            .page(1).size(10)
            .categorySlug("ai-ml").source("GITHUB")
            .minScore(4).keyword("rust")
            .build();

        String key = filter.cacheKey();
        assertThat(key).contains("p1").contains("s10").contains("ai-ml")
            .contains("GITHUB").contains("ms4").contains("rust");
    }

    @Test
    @DisplayName("null 필드 시 기본값 적용")
    void cacheKeyWithNullFields() {
        TrendFilterRequest filter = TrendFilterRequest.builder().build();

        String key = filter.cacheKey();
        assertThat(key).contains("call").contains("srcall").contains("kw");
    }

    @Test
    @DisplayName("다른 필터 → 다른 캐시 키")
    void differentFiltersDifferentKeys() {
        TrendFilterRequest f1 = TrendFilterRequest.builder().categorySlug("ai-ml").build();
        TrendFilterRequest f2 = TrendFilterRequest.builder().categorySlug("java").build();

        assertThat(f1.cacheKey()).isNotEqualTo(f2.cacheKey());
    }
}

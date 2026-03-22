package com.mud.dto;

import com.mud.domain.entity.Category;
import com.mud.dto.response.CategoryResponse;
import com.mud.dto.response.TrendPageResponse;
import com.mud.dto.response.TrendItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryResponseTest {

    @Test
    @DisplayName("Category → CategoryResponse 매핑")
    void fromCategory() {
        Category category = Category.builder()
            .id(1L).slug("ai-ml").displayName("AI/ML").emoji("🤖").sortOrder(1).build();

        CategoryResponse response = CategoryResponse.from(category);
        assertThat(response.slug()).isEqualTo("ai-ml");
        assertThat(response.displayName()).isEqualTo("AI/ML");
        assertThat(response.emoji()).isEqualTo("🤖");
        assertThat(response.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("TrendPageResponse.from — 페이지네이션 매핑")
    void trendPageResponseFrom() {
        Page<TrendItemResponse> page = new PageImpl<>(
            List.of(), PageRequest.of(2, 10), 50
        );

        TrendPageResponse response = TrendPageResponse.from(page);
        assertThat(response.number()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(50);
        assertThat(response.totalPages()).isEqualTo(5);
    }
}

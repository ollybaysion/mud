package com.mud.service;

import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import com.mud.dto.request.TrendFilterRequest;
import com.mud.dto.response.CategoryResponse;
import com.mud.dto.response.TrendItemResponse;
import com.mud.dto.response.TrendPageResponse;
import com.mud.dto.response.TrendStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendService {

    private final TrendItemRepository trendItemRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "trends", key = "#filter.cacheKey()")
    public TrendPageResponse getTrends(TrendFilterRequest filter) {
        PageRequest pageable = PageRequest.of(filter.getPage(), filter.getSize());
        return TrendPageResponse.from(
            trendItemRepository.findWithFilters(
                filter.getCategorySlug(),
                filter.getSource(),
                filter.getMinScore(),
                filter.getKeyword(),
                pageable
            ).map(TrendItemResponse::from)
        );
    }

    @Cacheable(value = "trend-detail", key = "#id")
    public TrendItemResponse getTrendDetail(Long id) {
        return trendItemRepository.findById(id)
            .map(TrendItemResponse::from)
            .orElseThrow(() -> new IllegalArgumentException("Trend item not found: " + id));
    }

    @Cacheable(value = "categories")
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Cacheable(value = "stats")
    public TrendStatsResponse getStats() {
        long totalItems = trendItemRepository.countByAnalysisStatus(
            com.mud.domain.entity.TrendItem.AnalysisStatus.DONE
        );

        Map<String, Long> bySource = new HashMap<>();
        trendItemRepository.countBySource()
            .forEach(row -> bySource.put(row[0].toString(), (Long) row[1]));

        Map<String, Long> byCategory = new HashMap<>();
        trendItemRepository.countByCategory()
            .forEach(row -> byCategory.put(row[0].toString(), (Long) row[1]));

        return new TrendStatsResponse(totalItems, bySource, byCategory, LocalDateTime.now());
    }

    @CacheEvict(value = {"trends", "stats"}, allEntries = true)
    public void evictTrendCaches() {
        // Called after crawler + analysis completes
    }
}

package com.mud.service;

import com.mud.domain.repository.CategoryRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendServiceEvictTest {

    @Mock private TrendItemRepository trendItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private TrendService trendService;

    @Test
    @DisplayName("evictTrendCaches — 예외 없이 실행")
    void evictCachesNoError() {
        // @CacheEvict 어노테이션은 Spring Context에서만 동작하지만
        // 메서드 자체가 예외 없이 실행되는지 확인
        trendService.evictTrendCaches();
    }
}

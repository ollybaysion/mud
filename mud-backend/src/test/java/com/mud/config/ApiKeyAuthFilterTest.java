package com.mud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("유효한 API 키 → 인증 성공")
    void validApiKey() throws Exception {
        SecurityContextHolder.clearContext();
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/crawl");
        request.addHeader("X-API-Key", "test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("잘못된 API 키 → 인증 실패")
    void invalidApiKey() throws Exception {
        SecurityContextHolder.clearContext();
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/crawl");
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("API 키 미설정(빈값) → 모든 요청 인증 실패")
    void emptyApiKey() throws Exception {
        SecurityContextHolder.clearContext();
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/crawl");
        request.addHeader("X-API-Key", "any-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("공개 API 경로 → 필터 스킵")
    void publicEndpointSkipsFilter() {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/trends");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Admin 경로 → 필터 적용")
    void adminEndpointAppliesFilter() {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("test-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/crawl");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}

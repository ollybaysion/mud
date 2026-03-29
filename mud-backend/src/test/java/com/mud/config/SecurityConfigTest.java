package com.mud.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.mud.api.controller.TrendController;
import com.mud.service.AnalysisService;
import com.mud.service.TrendService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TrendController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "admin.api-key=test-key",
    "cors.allowed-origins=http://localhost:3000,http://test.example.com"
})
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TrendService trendService;
    @MockBean private AnalysisService analysisService;

    @Test
    @DisplayName("공개 API는 인증 없이 접근 가능")
    void publicEndpointsAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CORS — 허용된 origin")
    void corsAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/trends")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("CORS — 허용되지 않은 origin")
    void corsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/trends")
                .header("Origin", "http://evil.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("비테스트 프로파일 + 빈 API 키 → 시작 실패")
    void emptyApiKeyInNonTestProfile() {
        MockEnvironment env = new MockEnvironment();
        // 프로파일 없음 = 프로덕션 환경
        SecurityConfig config = new SecurityConfig(env);

        assertThatThrownBy(config::validateAdminApiKey)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ADMIN_API_KEY");
    }

    @Test
    @DisplayName("테스트 프로파일 + 빈 API 키 → 정상 시작")
    void emptyApiKeyInTestProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        SecurityConfig config = new SecurityConfig(env);

        assertThatNoException().isThrownBy(config::validateAdminApiKey);
    }
}

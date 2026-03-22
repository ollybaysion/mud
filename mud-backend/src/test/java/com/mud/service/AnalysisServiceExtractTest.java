package com.mud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisServiceExtractTest {

    private Object service;
    private Method extractResponseText;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = AnalysisService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        service = constructor.newInstance(null, null, null, null, null, null, null, null);

        extractResponseText = AnalysisService.class.getDeclaredMethod("extractResponseText", Map.class);
        extractResponseText.setAccessible(true);
    }

    private String extract(Map<?, ?> response) throws Exception {
        return (String) extractResponseText.invoke(service, response);
    }

    @Test
    @DisplayName("정상 응답에서 텍스트 추출")
    void extractsTextFromValidResponse() throws Exception {
        Map<String, Object> response = Map.of(
            "content", List.of(Map.of("type", "text", "text", "분석 결과"))
        );

        String result = extract(response);
        assertThat(result).isEqualTo("분석 결과");
    }

    @Test
    @DisplayName("content가 null → RuntimeException")
    void throwsOnNullContent() {
        Map<String, Object> response = Map.of("id", "msg_123");

        assertThatThrownBy(() -> extract(response))
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("content가 빈 리스트 → RuntimeException")
    void throwsOnEmptyContent() {
        Map<String, Object> response = Map.of("content", List.of());

        assertThatThrownBy(() -> extract(response))
            .hasCauseInstanceOf(RuntimeException.class);
    }
}

package com.mud.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("IllegalArgumentException → 404 + 에러 메시지")
    void handleNotFound() {
        var response = handler.handleNotFound(new IllegalArgumentException("not found"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("error", "not found");
    }

    @Test
    @DisplayName("일반 예외 → 500 + Internal server error")
    void handleGeneric() {
        var response = handler.handleGeneric(new RuntimeException("unexpected"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
    }
}

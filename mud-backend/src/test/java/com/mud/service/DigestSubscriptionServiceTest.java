package com.mud.service;

import com.mud.domain.entity.DigestSubscriber;
import com.mud.domain.repository.DigestSubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigestSubscriptionServiceTest {

    @Mock private DigestSubscriberRepository subscriberRepository;
    @Mock private EmailService emailService;
    @InjectMocks private DigestSubscriptionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "https://test.example.com");
    }

    @Test
    @DisplayName("신규 구독 — 인증 이메일 발송")
    void subscribeNew() {
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(subscriberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = service.subscribe("test@example.com");

        assertThat(result).contains("인증 이메일");
        verify(emailService).sendHtmlEmail(eq("test@example.com"), any(), any());
    }

    @Test
    @DisplayName("이미 활성 구독자 — 스킵")
    void subscribeAlreadyActive() {
        DigestSubscriber existing = DigestSubscriber.builder()
            .email("test@example.com").active(true).unsubscribeToken("tok").build();
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));

        String result = service.subscribe("test@example.com");

        assertThat(result).contains("이미 구독 중");
        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    @DisplayName("인증 성공")
    void verifySuccess() {
        DigestSubscriber sub = DigestSubscriber.builder()
            .email("test@example.com").verificationToken("token123").unsubscribeToken("unsub").build();
        when(subscriberRepository.findByVerificationToken("token123")).thenReturn(Optional.of(sub));

        String result = service.verify("token123");

        assertThat(result).contains("확인");
        assertThat(sub.isActive()).isTrue();
    }

    @Test
    @DisplayName("잘못된 인증 토큰")
    void verifyInvalidToken() {
        when(subscriberRepository.findByVerificationToken("bad")).thenReturn(Optional.empty());

        String result = service.verify("bad");

        assertThat(result).contains("유효하지 않은");
    }

    @Test
    @DisplayName("구독 해지")
    void unsubscribe() {
        DigestSubscriber sub = DigestSubscriber.builder()
            .email("test@example.com").active(true).unsubscribeToken("unsub123").build();
        when(subscriberRepository.findByUnsubscribeToken("unsub123")).thenReturn(Optional.of(sub));

        String result = service.unsubscribe("unsub123");

        assertThat(result).contains("해지");
        assertThat(sub.isActive()).isFalse();
    }
}

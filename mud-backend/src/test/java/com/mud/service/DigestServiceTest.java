package com.mud.service;

import com.mud.domain.repository.DailyDigestRepository;
import com.mud.domain.repository.DigestSubscriberRepository;
import com.mud.domain.repository.TrendItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigestServiceTest {

    @Mock private TrendItemRepository trendItemRepository;
    @Mock private DigestSubscriberRepository subscriberRepository;
    @Mock private DailyDigestRepository dailyDigestRepository;
    @Mock private EmailService emailService;
    @InjectMocks private DigestService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "https://test.example.com");
    }

    @Test
    @DisplayName("이미 발송된 날짜 → 스킵")
    void skipAlreadySent() {
        when(dailyDigestRepository.existsByDigestDate(any(LocalDate.class))).thenReturn(true);

        service.sendDailyDigest();

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    @DisplayName("트렌드 0건 → 스킵")
    void skipNoItems() {
        when(dailyDigestRepository.existsByDigestDate(any())).thenReturn(false);
        when(trendItemRepository.findByStatusAndPeriodWithCategory(any(), any(), any()))
            .thenReturn(List.of());

        service.sendDailyDigest();

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    @DisplayName("활성 구독자 0명 → 스킵")
    void skipNoSubscribers() {
        when(dailyDigestRepository.existsByDigestDate(any())).thenReturn(false);
        when(trendItemRepository.findByStatusAndPeriodWithCategory(any(), any(), any()))
            .thenReturn(List.of(mock(com.mud.domain.entity.TrendItem.class)));
        when(subscriberRepository.findByActiveTrue()).thenReturn(List.of());

        service.sendDailyDigest();

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }
}

package com.mud.service;

import com.mud.domain.entity.CrawlerRun;
import com.mud.domain.repository.CrawlerRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerMonitorServiceTest {

    @Mock private CrawlerRunRepository crawlerRunRepository;
    @Mock private EmailService emailService;
    @InjectMocks private CrawlerMonitorService service;

    @Test
    @DisplayName("성공 기록")
    void recordSuccess() {
        when(crawlerRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordRun("GITHUB", LocalDateTime.now(), 10, null);

        verify(crawlerRunRepository).save(argThat(run -> "OK".equals(run.getStatus())));
    }

    @Test
    @DisplayName("실패 기록")
    void recordFailure() {
        when(crawlerRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(crawlerRunRepository.countBySourceAndStatusAndStartedAtGreaterThan(any(), any(), any()))
            .thenReturn(1L);

        service.recordRun("GITHUB", LocalDateTime.now(), 0, "timeout");

        verify(crawlerRunRepository).save(argThat(run -> "FAILED".equals(run.getStatus())));
    }

    @Test
    @DisplayName("연속 3회 실패 → 알림 이메일")
    void alertOnConsecutiveFailures() {
        when(crawlerRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(crawlerRunRepository.countBySourceAndStatusAndStartedAtGreaterThan(any(), any(), any()))
            .thenReturn(3L);
        when(crawlerRunRepository.findTopBySourceAndStatusOrderByStartedAtDesc(any(), eq("OK")))
            .thenReturn(java.util.Optional.empty());

        service.recordRun("HACKADAY", LocalDateTime.now(), 0, "Connection refused");

        verify(emailService).sendHtmlEmail(any(), contains("HACKADAY"), any());
    }

    @Test
    @DisplayName("상태 조회 — 빈 결과")
    void statusEmpty() {
        when(crawlerRunRepository.findLatestBySource()).thenReturn(List.of());

        Map<String, Object> status = service.getCrawlerStatus();

        assertThat(status.get("crawlers")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("상태 조회 — 정상")
    void statusWithData() {
        CrawlerRun run = CrawlerRun.builder()
            .id(1L).source("GITHUB").startedAt(LocalDateTime.now())
            .finishedAt(LocalDateTime.now()).status("OK").itemsCollected(10).build();
        when(crawlerRunRepository.findLatestBySource()).thenReturn(List.of(run));
        when(crawlerRunRepository.findTopBySourceAndStatusOrderByStartedAtDesc("GITHUB", "OK"))
            .thenReturn(java.util.Optional.of(run));

        Map<String, Object> status = service.getCrawlerStatus();

        assertThat(((List<?>) status.get("crawlers"))).hasSize(1);
    }
}

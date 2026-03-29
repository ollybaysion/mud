package com.mud.service;

import com.mud.domain.entity.CrawlerRun;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.CrawlerRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlerMonitorService {

    private final CrawlerRunRepository crawlerRunRepository;
    private final EmailService emailService;

    public void recordRun(String source, LocalDateTime startedAt, int itemsCollected, String errorMessage) {
        String status = errorMessage == null ? "OK" : "FAILED";

        CrawlerRun run = CrawlerRun.builder()
            .source(source)
            .startedAt(startedAt)
            .finishedAt(LocalDateTime.now())
            .status(status)
            .itemsCollected(itemsCollected)
            .errorMessage(errorMessage)
            .build();

        crawlerRunRepository.save(run);

        if ("FAILED".equals(status)) {
            checkConsecutiveFailures(source, errorMessage);
        }
    }

    private void checkConsecutiveFailures(String source, String errorMessage) {
        long recentFailures = crawlerRunRepository.countBySourceAndStatusAndStartedAtGreaterThan(
            source, "FAILED", LocalDateTime.now().minusHours(6));

        if (recentFailures >= 3) {
            var lastSuccess = crawlerRunRepository.findTopBySourceAndStatusOrderByStartedAtDesc(source, "OK");
            String lastSuccessAt = lastSuccess.map(r -> r.getFinishedAt().toString()).orElse("없음");

            String html = """
                <div style="font-family:sans-serif;padding:20px;">
                    <h2 style="color:#e11d48;">⚠️ 크롤러 연속 실패 알림</h2>
                    <p><strong>소스:</strong> %s</p>
                    <p><strong>연속 실패:</strong> %d회 (최근 6시간)</p>
                    <p><strong>마지막 성공:</strong> %s</p>
                    <p><strong>에러:</strong> %s</p>
                </div>
                """.formatted(source, recentFailures, lastSuccessAt, errorMessage);

            emailService.sendHtmlEmail("ollybaysion@gmail.com", "⚠️ Mud 크롤러 실패: " + source, html);
            log.warn("크롤러 연속 실패 알림 발송: source={}, failures={}", source, recentFailures);
        }
    }

    public Map<String, Object> getCrawlerStatus() {
        List<CrawlerRun> latestRuns = crawlerRunRepository.findLatestBySource();

        List<Map<String, Object>> crawlers = latestRuns.stream().map(run -> {
            var lastSuccess = crawlerRunRepository.findTopBySourceAndStatusOrderByStartedAtDesc(run.getSource(), "OK");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", run.getSource());
            map.put("lastRunAt", run.getStartedAt().toString());
            map.put("lastSuccessAt", lastSuccess.map(r -> r.getFinishedAt().toString()).orElse(null));
            map.put("lastFailedAt", "FAILED".equals(run.getStatus()) ? run.getStartedAt().toString() : null);
            map.put("itemsCollected", run.getItemsCollected());
            map.put("status", run.getStatus());
            map.put("errorMessage", run.getErrorMessage());
            return map;
        }).toList();

        long okCount = crawlers.stream().filter(c -> "OK".equals(c.get("status"))).count();
        long failedCount = crawlers.stream().filter(c -> "FAILED".equals(c.get("status"))).count();

        return Map.of(
            "crawlers", crawlers,
            "summary", Map.of(
                "total", crawlers.size(),
                "ok", okCount,
                "failed", failedCount,
                "lastCheckedAt", LocalDateTime.now().toString()
            )
        );
    }

    public Map<String, Object> getCrawlerHistory(LocalDate from, LocalDate to, String status, String source) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        List<CrawlerRun> runs;
        if (status != null && !status.isBlank()) {
            runs = crawlerRunRepository.findByStatusAndStartedAtBetweenOrderByStartedAtDesc(status, fromDt, toDt);
        } else {
            runs = crawlerRunRepository.findByStartedAtBetweenOrderByStartedAtDesc(fromDt, toDt);
        }

        if (source != null && !source.isBlank()) {
            runs = runs.stream().filter(r -> source.equals(r.getSource())).toList();
        }

        List<Map<String, Object>> history = runs.stream().map(run -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", run.getSource());
            map.put("startedAt", run.getStartedAt().toString());
            map.put("finishedAt", run.getFinishedAt() != null ? run.getFinishedAt().toString() : null);
            map.put("status", run.getStatus());
            map.put("itemsCollected", run.getItemsCollected());
            map.put("errorMessage", run.getErrorMessage());
            return map;
        }).toList();

        long okCount = history.stream().filter(h -> "OK".equals(h.get("status"))).count();
        long failedCount = history.stream().filter(h -> "FAILED".equals(h.get("status"))).count();

        return Map.of(
            "from", from.toString(),
            "to", to.toString(),
            "runs", history,
            "summary", Map.of("total", history.size(), "ok", okCount, "failed", failedCount)
        );
    }
}

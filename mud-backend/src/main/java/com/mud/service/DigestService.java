package com.mud.service;

import com.mud.domain.entity.DailyDigest;
import com.mud.domain.entity.DigestSubscriber;
import com.mud.domain.entity.TrendItem;
import com.mud.domain.repository.DailyDigestRepository;
import com.mud.domain.repository.DigestSubscriberRepository;
import com.mud.domain.repository.TrendItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigestService {

    private final TrendItemRepository trendItemRepository;
    private final DigestSubscriberRepository subscriberRepository;
    private final DailyDigestRepository dailyDigestRepository;
    private final EmailService emailService;

    @Value("${digest.base-url:https://mud-production-5786.up.railway.app}")
    private String baseUrl;

    @Transactional
    public void sendDailyDigest() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (dailyDigestRepository.existsByDigestDate(yesterday)) {
            log.info("데일리 다이제스트 이미 발송됨: {}", yesterday);
            return;
        }

        LocalDateTime startDt = yesterday.atStartOfDay();
        LocalDateTime endDt = yesterday.atTime(23, 59, 59);

        List<TrendItem> topItems = trendItemRepository
            .findByStatusAndPeriodWithCategory(TrendItem.AnalysisStatus.DONE, startDt, endDt)
            .stream()
            .filter(item -> item.getScoreTotal() != null)
            .limit(10)
            .toList();

        if (topItems.isEmpty()) {
            log.info("데일리 다이제스트: 전날 트렌드 0건, 발송 스킵");
            return;
        }

        List<DigestSubscriber> subscribers = subscriberRepository.findByActiveTrue();
        if (subscribers.isEmpty()) {
            log.info("데일리 다이제스트: 활성 구독자 0명, 발송 스킵");
            return;
        }

        String dateLabel = yesterday.format(DateTimeFormatter.ofPattern("M월 d일"));
        String subject = "⚗️ Mud 데일리 트렌드 — " + dateLabel;

        int sentCount = 0;
        for (DigestSubscriber subscriber : subscribers) {
            String html = buildDigestHtml(topItems, dateLabel, subscriber.getUnsubscribeToken());
            emailService.sendHtmlEmail(subscriber.getEmail(), subject, html);
            sentCount++;
        }

        DailyDigest digest = DailyDigest.builder()
            .digestDate(yesterday)
            .itemCount(topItems.size())
            .sentCount(sentCount)
            .build();
        dailyDigestRepository.save(digest);

        log.info("데일리 다이제스트 발송 완료: date={}, items={}, sent={}", yesterday, topItems.size(), sentCount);
    }

    private String buildDigestHtml(List<TrendItem> items, String dateLabel, String unsubscribeToken) {
        String itemsHtml = items.stream().map(item -> {
            String category = item.getCategory() != null ? item.getCategory().getDisplayName() : "일반";
            String summary = item.getKoreanSummary() != null ? item.getKoreanSummary() : "";
            String score = item.getScoreTotal() != null ? String.valueOf(item.getScoreTotal()) : "?";
            String url = baseUrl.replace("mud-production-5786.up.railway.app", "mud-frontend-production.up.railway.app")
                + "/trends/" + item.getId();

            return """
                <tr>
                    <td style="padding: 12px 0; border-bottom: 1px solid #eee;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
                            <span style="color: #6366f1; font-weight: 600; font-size: 12px;">★%s</span>
                            <span style="font-size: 12px; color: #888;">%s</span>
                        </div>
                        <a href="%s" style="font-size: 14px; font-weight: 600; color: #111; text-decoration: none;">%s</a>
                        <p style="font-size: 13px; color: #555; margin: 4px 0 0; line-height: 1.5;">%s</p>
                    </td>
                </tr>
                """.formatted(score, category, url, item.getTitle(), summary);
        }).collect(Collectors.joining());

        String unsubscribeUrl = baseUrl + "/api/digest/unsubscribe?token=" + unsubscribeToken;

        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #fff;">
                <h1 style="font-size: 18px; color: #6366f1; margin-bottom: 4px;">⚗️ Mud 데일리 트렌드</h1>
                <p style="font-size: 13px; color: #888; margin-bottom: 20px;">%s · 상위 %d건</p>
                <table style="width: 100%%; border-collapse: collapse;">
                    %s
                </table>
                <div style="margin-top: 24px; padding-top: 16px; border-top: 1px solid #eee; font-size: 11px; color: #aaa; text-align: center;">
                    <a href="%s" style="color: #aaa;">구독 해지</a> · Mud — 기술 트렌드 큐레이션
                </div>
            </div>
            """.formatted(dateLabel, items.size(), itemsHtml, unsubscribeUrl);
    }
}

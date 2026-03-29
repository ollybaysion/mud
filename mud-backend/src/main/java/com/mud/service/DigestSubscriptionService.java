package com.mud.service;

import com.mud.domain.entity.DigestSubscriber;
import com.mud.domain.repository.DigestSubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigestSubscriptionService {

    private final DigestSubscriberRepository subscriberRepository;
    private final EmailService emailService;

    @Value("${digest.base-url:https://mud-production-5786.up.railway.app}")
    private String baseUrl;

    @Transactional
    public String subscribe(String email) {
        var existing = subscriberRepository.findByEmail(email);
        if (existing.isPresent()) {
            DigestSubscriber sub = existing.get();
            if (sub.isActive()) {
                return "이미 구독 중입니다.";
            }
            // 재구독: 새 인증 토큰 발급
            sub.setVerificationToken(UUID.randomUUID().toString());
            sub.setUnsubscribedAt(null);
            subscriberRepository.save(sub);
            sendVerificationEmail(sub);
            return "인증 이메일을 발송했습니다. 이메일을 확인해주세요.";
        }

        DigestSubscriber subscriber = DigestSubscriber.builder()
            .email(email)
            .verificationToken(UUID.randomUUID().toString())
            .unsubscribeToken(UUID.randomUUID().toString())
            .build();

        subscriberRepository.save(subscriber);
        sendVerificationEmail(subscriber);
        log.info("새 구독 신청: {}", email);
        return "인증 이메일을 발송했습니다. 이메일을 확인해주세요.";
    }

    @Transactional
    public String verify(String token) {
        var subscriber = subscriberRepository.findByVerificationToken(token)
            .orElse(null);

        if (subscriber == null) {
            return "유효하지 않은 인증 링크입니다.";
        }

        subscriber.setActive(true);
        subscriber.setVerifiedAt(LocalDateTime.now());
        subscriber.setVerificationToken(null);
        subscriberRepository.save(subscriber);

        log.info("구독 인증 완료: {}", subscriber.getEmail());
        return "구독이 확인되었습니다! 매일 아침 트렌드 다이제스트를 받아보세요.";
    }

    @Transactional
    public String unsubscribe(String token) {
        var subscriber = subscriberRepository.findByUnsubscribeToken(token)
            .orElse(null);

        if (subscriber == null) {
            return "유효하지 않은 구독 해지 링크입니다.";
        }

        subscriber.setActive(false);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);

        log.info("구독 해지: {}", subscriber.getEmail());
        return "구독이 해지되었습니다.";
    }

    private void sendVerificationEmail(DigestSubscriber subscriber) {
        String verifyUrl = baseUrl + "/api/digest/verify?token=" + subscriber.getVerificationToken();
        String html = """
            <div style="font-family: -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #6366f1;">⚗️ Mud 데일리 다이제스트</h2>
                <p>아래 버튼을 클릭하여 구독을 확인해주세요.</p>
                <a href="%s" style="display: inline-block; padding: 12px 24px; background: #6366f1; color: #fff; text-decoration: none; border-radius: 6px; font-weight: 600;">
                    구독 확인하기
                </a>
                <p style="margin-top: 20px; font-size: 12px; color: #888;">
                    이 이메일을 요청하지 않았다면 무시해주세요.
                </p>
            </div>
            """.formatted(verifyUrl);

        emailService.sendHtmlEmail(subscriber.getEmail(), "⚗️ Mud 데일리 다이제스트 — 구독 확인", html);
    }
}

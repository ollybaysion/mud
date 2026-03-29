package com.mud.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;

    @Test
    @DisplayName("HTML 이메일 발송 성공")
    void sendHtmlEmailSuccess() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@mud.dev");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("user@example.com", "테스트 제목", "<h1>테스트</h1>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패 — 예외 삼키고 로그만")
    void sendHtmlEmailFailure() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@mud.dev");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("SMTP error"))
            .when(mailSender).send(any(MimeMessage.class));

        // 예외 없이 종료
        emailService.sendHtmlEmail("user@example.com", "제목", "<p>내용</p>");
    }
}

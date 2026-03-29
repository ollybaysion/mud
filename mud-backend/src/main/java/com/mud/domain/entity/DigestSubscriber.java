package com.mud.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "digest_subscribers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DigestSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "unsubscribe_token", nullable = false)
    private String unsubscribeToken;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "subscribed_at", nullable = false)
    @Builder.Default
    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
}

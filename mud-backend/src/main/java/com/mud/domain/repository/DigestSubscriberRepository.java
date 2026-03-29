package com.mud.domain.repository;

import com.mud.domain.entity.DigestSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DigestSubscriberRepository extends JpaRepository<DigestSubscriber, Long> {

    Optional<DigestSubscriber> findByEmail(String email);

    Optional<DigestSubscriber> findByVerificationToken(String token);

    Optional<DigestSubscriber> findByUnsubscribeToken(String token);

    List<DigestSubscriber> findByActiveTrue();

    long countByActiveTrue();
}

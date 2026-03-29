package com.mud.domain.repository;

import com.mud.domain.entity.DailyDigest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyDigestRepository extends JpaRepository<DailyDigest, Long> {

    boolean existsByDigestDate(LocalDate date);

    void deleteByDigestDate(LocalDate date);
}

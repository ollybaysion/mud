package com.mud.domain.repository;

import com.mud.domain.entity.DailyDigest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DailyDigestRepository extends JpaRepository<DailyDigest, Long> {

    boolean existsByDigestDate(LocalDate date);

    @Modifying
    @Query("DELETE FROM DailyDigest d WHERE d.digestDate = :date")
    void deleteByDigestDate(@Param("date") LocalDate date);
}

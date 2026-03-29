package com.mud.domain.repository;

import com.mud.domain.entity.CrawlerRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrawlerRunRepository extends JpaRepository<CrawlerRun, Long> {

    Optional<CrawlerRun> findTopBySourceOrderByStartedAtDesc(String source);

    Optional<CrawlerRun> findTopBySourceAndStatusOrderByStartedAtDesc(String source, String status);

    @Query("SELECT r FROM CrawlerRun r WHERE r.id IN (SELECT MAX(r2.id) FROM CrawlerRun r2 GROUP BY r2.source)")
    List<CrawlerRun> findLatestBySource();

    long countBySourceAndStatusAndStartedAtGreaterThan(String source, String status, java.time.LocalDateTime after);
}

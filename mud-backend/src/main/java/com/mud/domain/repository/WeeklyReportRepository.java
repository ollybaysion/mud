package com.mud.domain.repository;

import com.mud.domain.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findTopByOrderByPeriodStartDesc();

    Optional<WeeklyReport> findByPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
        LocalDate date1, LocalDate date2
    );

    boolean existsByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    void deleteByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);
}

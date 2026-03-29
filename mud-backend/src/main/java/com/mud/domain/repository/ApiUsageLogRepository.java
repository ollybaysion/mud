package com.mud.domain.repository;

import com.mud.domain.entity.ApiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, Long> {

    List<ApiUsageLog> findByCalledAtAfterOrderByCalledAtDesc(LocalDateTime since);

    @Query("""
        SELECT a.apiType, COUNT(a), SUM(a.inputTokens), SUM(a.outputTokens), SUM(a.estimatedCost)
        FROM ApiUsageLog a
        WHERE a.calledAt >= :since
        GROUP BY a.apiType
        """)
    List<Object[]> summarizeByType(LocalDateTime since);

    @Query("""
        SELECT CAST(a.calledAt AS LocalDate), COUNT(a), SUM(a.inputTokens), SUM(a.outputTokens), SUM(a.estimatedCost)
        FROM ApiUsageLog a
        WHERE a.calledAt >= :since
        GROUP BY CAST(a.calledAt AS LocalDate)
        ORDER BY CAST(a.calledAt AS LocalDate) DESC
        """)
    List<Object[]> dailyTrend(LocalDateTime since);
}

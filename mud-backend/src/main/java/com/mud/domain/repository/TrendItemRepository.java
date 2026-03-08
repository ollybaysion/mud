package com.mud.domain.repository;

import com.mud.domain.entity.TrendItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrendItemRepository extends JpaRepository<TrendItem, Long> {

    boolean existsByUrlHash(String urlHash);

    List<TrendItem> findTop20ByAnalysisStatusOrderByCrawledAtAsc(TrendItem.AnalysisStatus status);

    @Query("""
        SELECT t FROM TrendItem t
        LEFT JOIN FETCH t.category c
        WHERE t.analysisStatus = 'DONE'
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (:source IS NULL OR CAST(t.source AS string) = :source)
        AND t.relevanceScore >= :minScore
        AND (
            CAST(:keyword AS string) IS NULL
            OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
            OR LOWER(t.koreanSummary) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
        )
        ORDER BY t.publishedAt DESC NULLS LAST, t.relevanceScore DESC
        """)
    Page<TrendItem> findWithFilters(
        @Param("categorySlug") String categorySlug,
        @Param("source") String source,
        @Param("minScore") int minScore,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        SELECT t.source as source, COUNT(t) as count
        FROM TrendItem t
        WHERE t.analysisStatus = 'DONE'
        GROUP BY t.source
        """)
    List<Object[]> countBySource();

    @Query("""
        SELECT c.slug as slug, COUNT(t) as count
        FROM TrendItem t
        JOIN t.category c
        WHERE t.analysisStatus = 'DONE'
        GROUP BY c.slug
        """)
    List<Object[]> countByCategory();

    long countByAnalysisStatus(TrendItem.AnalysisStatus status);
}

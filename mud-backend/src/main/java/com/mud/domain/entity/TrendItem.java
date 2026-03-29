package com.mud.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "trend_items",
    indexes = {
        @Index(name = "idx_trend_published_at",    columnList = "published_at DESC"),
        @Index(name = "idx_trend_category",         columnList = "category_id"),
        @Index(name = "idx_trend_source",           columnList = "source"),
        @Index(name = "idx_trend_relevance_score",  columnList = "relevance_score DESC"),
        @Index(name = "idx_trend_analysis_status",  columnList = "analysis_status"),
        @Index(name = "idx_trend_url_hash",         columnList = "url_hash", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrendItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(nullable = false, length = 64, unique = true)
    private String urlHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CrawlSource source;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String koreanSummary;

    @Column(columnDefinition = "TEXT")
    private String deepAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "relevance_score")
    private Integer relevanceScore;

    @ElementCollection
    @CollectionTable(
        name = "trend_keywords",
        joinColumns = @JoinColumn(name = "trend_item_id")
    )
    @Column(name = "keyword", length = 50)
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    private Integer githubStars;

    @Column(length = 50)
    private String githubLanguage;

    @Column(length = 50)
    private String arxivId;

    @Column(name = "topic_tag", length = 100)
    private String topicTag;

    @Column(name = "score_relevance")
    private Short scoreRelevance;

    @Column(name = "score_actionability")
    private Short scoreActionability;

    @Column(name = "score_impact")
    private Short scoreImpact;

    @Column(name = "score_timeliness")
    private Short scoreTimeliness;

    @Column(name = "score_total")
    private Short scoreTotal;

    public enum AnalysisStatus {
        PENDING, PROCESSING, DONE, FAILED, REJECTED
    }

    public enum CrawlSource {
        GITHUB, HACKER_NEWS, DEV_TO, ARXIV, REDDIT,
        PAPERS_WITH_CODE, INFOQ, HUGGING_FACE, LOBSTERS,
        INSIDE_JAVA, ISOCPP, TLDR_AI, THE_NEW_STACK,
        CNCF, STACKOVERFLOW_BLOG, MARTIN_FOWLER, JETBRAINS,
        GEEKNEWS, NVIDIA_BLOG, SERVE_THE_HOME, TOMS_HARDWARE,
        PHORONIX, TECHPOWERUP, HACKADAY, EE_TIMES,
        SEMI_ENGINEERING, CHIPS_AND_CHEESE, CNX_SOFTWARE,
        THE_NEXT_PLATFORM, HPCWIRE, IEEE_SPECTRUM,
        NAVER_D2, KAKAO_TECH, TOSS_TECH,
        LINE_ENGINEERING, DAANGN, COUPANG_ENGINEERING,
        @Deprecated WOOWAHAN, @Deprecated VIDEOCARDZ
    }
}

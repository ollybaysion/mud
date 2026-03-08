-- Categories table
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    emoji       VARCHAR(10),
    sort_order  INT DEFAULT 0
);

-- Trend items table
CREATE TABLE trend_items (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(500) NOT NULL,
    original_url     TEXT NOT NULL,
    url_hash         VARCHAR(64) NOT NULL UNIQUE,
    source           VARCHAR(30) NOT NULL,
    description      TEXT,
    korean_summary   TEXT,
    category_id      BIGINT REFERENCES categories(id),
    relevance_score  INT CHECK (relevance_score BETWEEN 1 AND 5),
    published_at     TIMESTAMP,
    crawled_at       TIMESTAMP NOT NULL,
    analyzed_at      TIMESTAMP,
    analysis_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    github_stars     INT,
    github_language  VARCHAR(50),
    arxiv_id         VARCHAR(50)
);

-- Keywords (ElementCollection)
CREATE TABLE trend_keywords (
    trend_item_id BIGINT NOT NULL REFERENCES trend_items(id) ON DELETE CASCADE,
    keyword       VARCHAR(50) NOT NULL
);

-- Indexes
CREATE INDEX idx_trend_published_at     ON trend_items (published_at DESC);
CREATE INDEX idx_trend_category         ON trend_items (category_id);
CREATE INDEX idx_trend_source           ON trend_items (source);
CREATE INDEX idx_trend_relevance_score  ON trend_items (relevance_score DESC);
CREATE INDEX idx_trend_analysis_status  ON trend_items (analysis_status);
CREATE INDEX idx_trend_crawled_at       ON trend_items (crawled_at DESC);

CREATE TABLE weekly_reports (
    id BIGSERIAL PRIMARY KEY,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_count INT NOT NULL,
    highlights_json TEXT NOT NULL,
    category_stats_json TEXT,
    ai_summary TEXT,
    ai_model VARCHAR(50),
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(period_start, period_end)
);

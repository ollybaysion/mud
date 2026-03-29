CREATE TABLE crawler_runs (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    items_collected INT DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_crawler_runs_source ON crawler_runs (source);
CREATE INDEX idx_crawler_runs_started ON crawler_runs (started_at DESC);

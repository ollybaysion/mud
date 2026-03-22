-- pg_trgm 확장 활성화 (LIKE '%keyword%' 검색에 GIN 인덱스 사용 가능)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- title, korean_summary에 GIN 인덱스 추가
CREATE INDEX idx_trend_title_trgm ON trend_items USING gin (title gin_trgm_ops);
CREATE INDEX idx_trend_summary_trgm ON trend_items USING gin (korean_summary gin_trgm_ops);

-- 1. V6 deprecated scoring_* 컬럼 제거
ALTER TABLE trend_items DROP COLUMN IF EXISTS scoring_relevance;
ALTER TABLE trend_items DROP COLUMN IF EXISTS scoring_timeliness;
ALTER TABLE trend_items DROP COLUMN IF EXISTS scoring_actionability;
ALTER TABLE trend_items DROP COLUMN IF EXISTS scoring_impact;

-- 2. score_total 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_trend_score_total ON trend_items (score_total DESC NULLS LAST);

-- 3. GIN 트라이그램 인덱스를 lower() 기반으로 재생성
DROP INDEX IF EXISTS idx_trend_title_trgm;
DROP INDEX IF EXISTS idx_trend_summary_trgm;
CREATE INDEX idx_trend_title_trgm ON trend_items USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_trend_summary_trgm ON trend_items USING gin (lower(korean_summary) gin_trgm_ops);

-- 4. score_* 컬럼에 CHECK 제약 추가
ALTER TABLE trend_items ADD CONSTRAINT chk_score_relevance CHECK (score_relevance BETWEEN 0 AND 10);
ALTER TABLE trend_items ADD CONSTRAINT chk_score_actionability CHECK (score_actionability BETWEEN 0 AND 10);
ALTER TABLE trend_items ADD CONSTRAINT chk_score_impact CHECK (score_impact BETWEEN 0 AND 10);
ALTER TABLE trend_items ADD CONSTRAINT chk_score_timeliness CHECK (score_timeliness BETWEEN 0 AND 10);
ALTER TABLE trend_items ADD CONSTRAINT chk_score_total CHECK (score_total BETWEEN 0 AND 100);

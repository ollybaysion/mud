ALTER TABLE trend_items ADD COLUMN scoring_relevance SMALLINT;
ALTER TABLE trend_items ADD COLUMN scoring_timeliness SMALLINT;
ALTER TABLE trend_items ADD COLUMN scoring_actionability SMALLINT;
ALTER TABLE trend_items ADD COLUMN scoring_impact SMALLINT;
ALTER TABLE trend_items ADD COLUMN topic_tag VARCHAR(100);

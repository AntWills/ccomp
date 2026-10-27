CREATE INDEX idx_events_title_trgm
    ON tb_events USING gin (title gin_trgm_ops);

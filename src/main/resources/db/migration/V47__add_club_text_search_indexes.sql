CREATE INDEX idx_clubs_name_trgm
    ON tb_clubs USING gin (name gin_trgm_ops);

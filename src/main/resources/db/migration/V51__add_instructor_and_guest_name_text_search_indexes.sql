CREATE INDEX idx_event_instructors_name_trgm
    ON tb_event_instructors USING gin (name gin_trgm_ops);

CREATE INDEX idx_event_guests_name_trgm
    ON tb_event_guests USING gin (name gin_trgm_ops);

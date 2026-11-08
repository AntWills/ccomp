UPDATE tb_events
SET max_capacity = NULL
WHERE max_capacity < 0;

ALTER TABLE tb_events
    ADD COLUMN active_enrollment_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_events_max_capacity_non_negative
        CHECK (max_capacity IS NULL OR max_capacity >= 0),
    ADD CONSTRAINT chk_events_active_enrollment_count_non_negative
        CHECK (active_enrollment_count >= 0);

UPDATE tb_events event
SET active_enrollment_count = (
    SELECT COUNT(*)
    FROM tb_event_enrollments enrollment
    WHERE enrollment.events_id = event.id
      AND enrollment.status IN ('CONFIRMED', 'CHECKED_IN')
);

ALTER TABLE tb_event_activities
    ADD COLUMN max_capacity INTEGER,
    ADD CONSTRAINT chk_event_activities_max_capacity_non_negative
        CHECK (max_capacity IS NULL OR max_capacity >= 0);

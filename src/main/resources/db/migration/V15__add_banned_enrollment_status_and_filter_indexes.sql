ALTER TABLE tb_event_enrollments
    DROP CONSTRAINT IF EXISTS tb_event_enrollments_status_check;

ALTER TABLE tb_event_enrollments
    ADD CONSTRAINT ck_event_enrollments_status
    CHECK (status IN ('CONFIRMED', 'CHECKED_IN', 'CANCELED', 'BANNED'));

DROP INDEX IF EXISTS idx_event_enrollment_created_at_id;
DROP INDEX IF EXISTS idx_enrollment_event_status;

CREATE INDEX idx_enrollments_event_created_at_id
    ON tb_event_enrollments (events_id, created_at DESC, id DESC);

CREATE INDEX idx_enrollments_event_status_created_at_id
    ON tb_event_enrollments (events_id, status, created_at DESC, id DESC);

ALTER TABLE tb_event_activities
    ADD COLUMN active_enrollment_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_event_activities_active_enrollment_count_non_negative
        CHECK (active_enrollment_count >= 0);

UPDATE tb_event_activities activity
SET active_enrollment_count = (
    SELECT COUNT(*)
    FROM tb_event_enrollment_activities enrollment_activity
    JOIN tb_event_enrollments enrollment
        ON enrollment.id = enrollment_activity.enrollment_id
    WHERE enrollment_activity.activity_id = activity.id
      AND enrollment.status IN ('CONFIRMED', 'CHECKED_IN')
);

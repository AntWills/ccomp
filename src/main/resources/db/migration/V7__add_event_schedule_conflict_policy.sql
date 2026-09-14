ALTER TABLE tb_events
    ADD COLUMN schedule_conflict_policy VARCHAR(20) NOT NULL
        CHECK (schedule_conflict_policy IN (
            'ALLOW',
            'PREVENT'
        ));

CREATE INDEX idx_event_activities_dates
    ON tb_event_activities (start_date, end_date);

CREATE INDEX idx_enrollment_user_status
    ON tb_event_enrollments (user_id, status);

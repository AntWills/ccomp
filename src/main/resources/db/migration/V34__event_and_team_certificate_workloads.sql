ALTER TABLE tb_events
    ADD COLUMN workload_hours NUMERIC,
    ADD COLUMN team_certificate_workload_hours NUMERIC,
    ADD CONSTRAINT chk_events_workload_hours_non_negative
        CHECK (workload_hours IS NULL OR workload_hours >= 0),
    ADD CONSTRAINT chk_events_team_certificate_workload_hours_non_negative
        CHECK (team_certificate_workload_hours IS NULL OR team_certificate_workload_hours >= 0);

ALTER TABLE tb_event_certificate_box_template
    ADD COLUMN template_type VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT';

ALTER TABLE tb_event_certificate_box_template
    DROP CONSTRAINT IF EXISTS tb_event_certificate_box_template_event_id_key,
    DROP CONSTRAINT IF EXISTS uk_event_certificate_box_template_event_id,
    ADD CONSTRAINT uk_event_certificate_box_type UNIQUE (event_id, template_type);

ALTER TABLE tb_event_certificate_validation_code
    ADD COLUMN certificate_type VARCHAR(20) NOT NULL DEFAULT 'EVENT';

UPDATE tb_event_certificate_validation_code
SET certificate_type = 'ACTIVITY'
WHERE activity_id IS NOT NULL;

DROP INDEX IF EXISTS uk_cert_event_user_null_activity;
DROP INDEX IF EXISTS uk_cert_activity_user;

CREATE UNIQUE INDEX uk_cert_validation_event_user_type
    ON tb_event_certificate_validation_code (event_id, user_id, certificate_type)
    WHERE activity_id IS NULL;

CREATE UNIQUE INDEX uk_cert_validation_activity_user_type
    ON tb_event_certificate_validation_code (activity_id, user_id, certificate_type)
    WHERE activity_id IS NOT NULL;

CREATE INDEX idx_event_instructors_user_status_activity
    ON tb_event_instructors (user_id, status, activity_id)
    WHERE user_id IS NOT NULL;

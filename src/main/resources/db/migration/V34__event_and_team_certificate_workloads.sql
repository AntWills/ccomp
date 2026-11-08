ALTER TABLE tb_events
    ADD COLUMN workload_hours NUMERIC,
    ADD COLUMN team_certificate_workload_hours NUMERIC,
    ADD COLUMN organization VARCHAR(255),
    ADD CONSTRAINT chk_events_workload_hours_non_negative
        CHECK (workload_hours IS NULL OR workload_hours >= 0),
    ADD CONSTRAINT chk_events_team_certificate_workload_hours_non_negative
        CHECK (team_certificate_workload_hours IS NULL OR team_certificate_workload_hours >= 0);

ALTER TABLE tb_event_certificate_box_template
    ADD COLUMN template_type VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT';

-- Keep the most recently saved template for each event/type pair.
WITH ranked_templates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY event_id, template_type
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM tb_event_certificate_box_template
)
DELETE FROM tb_event_certificate_box_template template
USING ranked_templates ranked
WHERE template.id = ranked.id
  AND ranked.row_number > 1;

ALTER TABLE tb_event_certificate_box_template
    DROP CONSTRAINT IF EXISTS tb_event_certificate_box_template_event_id_key,
    DROP CONSTRAINT IF EXISTS uk_event_certificate_box_template_event_id,
    ADD CONSTRAINT uk_event_certificate_box_type UNIQUE (event_id, template_type);

ALTER TABLE tb_event_certificate_validation_code
    ADD COLUMN certificate_type VARCHAR(20) NOT NULL DEFAULT 'EVENT';

UPDATE tb_event_certificate_validation_code
SET certificate_type = 'ACTIVITY'
WHERE activity_id IS NOT NULL;

-- Keep the oldest validation code for each event/user/type or activity/user/type.
WITH ranked_validation_codes AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY CASE WHEN activity_id IS NULL THEN event_id END,
                            CASE WHEN activity_id IS NOT NULL THEN activity_id END,
                            user_id, certificate_type
               ORDER BY created_at, id
           ) AS row_number
    FROM tb_event_certificate_validation_code
)
DELETE FROM tb_event_certificate_validation_code validation_code
USING ranked_validation_codes ranked
WHERE validation_code.id = ranked.id
  AND ranked.row_number > 1;

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

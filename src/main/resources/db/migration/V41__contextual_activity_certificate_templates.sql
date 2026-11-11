ALTER TABLE tb_event_activity_certificate_box_template
    DROP CONSTRAINT IF EXISTS tb_event_activity_certificate_box_template_event_id_key,
    ADD COLUMN template_type VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT';

ALTER TABLE tb_event_activity_certificate_box_template
    ADD CONSTRAINT uk_event_activity_certificate_box_context UNIQUE (event_id, template_type);

CREATE TABLE tb_activity_certificate_template (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES tb_event_activities(id) ON DELETE CASCADE,
    template_type VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    variables JSONB,
    boxes JSONB,
    cover_template_key VARCHAR(2048),
    CONSTRAINT uk_activity_certificate_template_context UNIQUE (activity_id, template_type),
    CONSTRAINT chk_activity_certificate_template_type
        CHECK (template_type IN ('PARTICIPANT', 'INSTRUCTOR', 'GUEST'))
);

CREATE INDEX idx_activity_certificate_template_activity
    ON tb_activity_certificate_template(activity_id);

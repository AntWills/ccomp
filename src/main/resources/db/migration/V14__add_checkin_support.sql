ALTER TABLE tb_event_enrollment_activities
    ADD COLUMN attended_at TIMESTAMP;

CREATE TABLE tb_events_check_in (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    code UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_events_check_in_activity_id UNIQUE (activity_id),
    CONSTRAINT fk_events_check_in_activity
        FOREIGN KEY (activity_id)
        REFERENCES tb_event_activities (id)
        ON DELETE CASCADE
);

-- Índice explicitado no @Index da entidade
CREATE INDEX idx_events_check_in_activity_id ON tb_events_check_in(activity_id);
CREATE TABLE tb_event_instructors (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    activity_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('CONFIRMED', 'CANCELED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_instructors_activity FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE,
    CONSTRAINT uk_event_instructor_activity_user UNIQUE (activity_id, user_id)
);
CREATE INDEX idx_event_instructors_activity_created ON tb_event_instructors (activity_id, created_at DESC, id DESC);

CREATE TABLE tb_event_instructor_invitations (
    id BIGSERIAL PRIMARY KEY,
    code UUID NOT NULL UNIQUE,
    activity_id BIGINT NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT'
        CHECK (status IN ('SENT', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    invited_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_event_instructor_invitations_activity FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE
);
CREATE INDEX idx_event_instructor_invitations_activity ON tb_event_instructor_invitations (activity_id);
CREATE INDEX idx_event_instructor_invitations_invited ON tb_event_instructor_invitations (invited_at DESC, id DESC);

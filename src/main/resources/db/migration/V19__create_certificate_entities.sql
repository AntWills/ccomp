CREATE TABLE tb_event_certificate_cover_template (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL UNIQUE,
    cover_template_key VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_cover_template_event FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE
);

CREATE TABLE tb_event_certificate_box_template (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL UNIQUE,
    boxes JSONB NOT NULL,
    variables JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_box_template_event FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE
);

CREATE TABLE tb_event_activity_certificate_box_template (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL UNIQUE,
    boxes JSONB NOT NULL,
    variables JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_activity_box_template_event FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE
);

CREATE TABLE tb_event_certificate_validation_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    event_id BIGINT NOT NULL,
    activity_id BIGINT,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_certificate_validation_code_event FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_certificate_validation_code_activity FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_certificate_validation_code_user FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_cert_event_user_null_activity
    ON tb_event_certificate_validation_code (event_id, user_id)
    WHERE activity_id IS NULL;

CREATE UNIQUE INDEX uk_cert_activity_user
    ON tb_event_certificate_validation_code (activity_id, user_id)
    WHERE activity_id IS NOT NULL;
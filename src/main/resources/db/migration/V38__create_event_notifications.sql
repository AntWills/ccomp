CREATE TABLE tb_event_notifications (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES tb_events (id) ON DELETE CASCADE,
    activity_id BIGINT REFERENCES tb_event_activities (id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES tb_users (id),
    audience VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recipient_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatch_completed_at TIMESTAMP,
    CONSTRAINT ck_event_notifications_audience CHECK (audience IN ('EVENT', 'ACTIVITY', 'SELECTED')),
    CONSTRAINT ck_event_notifications_scope CHECK ((audience = 'EVENT' AND activity_id IS NULL) OR (audience = 'ACTIVITY' AND activity_id IS NOT NULL) OR audience = 'SELECTED')
);

CREATE INDEX idx_event_notifications_event_created_id
    ON tb_event_notifications (event_id, created_at DESC, id DESC);

CREATE TABLE tb_event_notification_recipients (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES tb_event_notifications (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES tb_users (id),
    user_name VARCHAR(255) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    email_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_attempts INTEGER NOT NULL DEFAULT 0,
    email_error TEXT,
    last_email_attempt_at TIMESTAMP,
    email_sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_notification_recipient UNIQUE (notification_id, user_id),
    CONSTRAINT ck_event_notification_recipient_email_status CHECK (email_status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_event_notification_recipient_user_created_id
    ON tb_event_notification_recipients (user_id, created_at DESC, id DESC);
CREATE INDEX idx_event_notification_recipient_notification_status
    ON tb_event_notification_recipients (notification_id, email_status);

CREATE TABLE tb_event_notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES tb_event_notifications (id) ON DELETE CASCADE,
    message_type VARCHAR(20) NOT NULL,
    recipient_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    CONSTRAINT ck_event_notification_outbox_type CHECK (message_type IN ('DISPATCH', 'DELIVERY'))
);

CREATE INDEX idx_event_notification_outbox_pending ON tb_event_notification_outbox (id) WHERE published_at IS NULL;

-- 1. Convites do Evento
CREATE TABLE tb_event_invitations (
    id BIGSERIAL PRIMARY KEY,
    code UUID NOT NULL UNIQUE,
    event_id BIGINT NOT NULL,
    user_id UUID,
    email_address VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL
        CHECK (status IN (
            'PENDING',
            'ACCEPTED',
            'DECLINED',
            'CANCELLED'
        )),
    invited_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    CONSTRAINT fk_event_invitations_event FOREIGN KEY (event_id) REFERENCES tb_events (id)
);

CREATE INDEX idx_event_invitations_event ON tb_event_invitations (event_id);
CREATE INDEX idx_event_invitations_invited_id ON tb_event_invitations (invited_at DESC, id DESC);

-- 2. Convidados do Evento
CREATE TABLE tb_event_guests (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN (
            'CONFIRMED',
            'CANCELED'
        )),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
        CHECK (visibility IN (
            'PUBLIC',
            'PRIVATE'
        )),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tb_event_guests_event FOREIGN KEY (event_id) REFERENCES tb_events (id)
);

CREATE INDEX idx_event_guest_user_id_event_id ON tb_event_guests (user_id, event_id);
CREATE INDEX idx_event_guests_created_at_id ON tb_event_guests (created_at DESC, id DESC);

-- 3. Convidados das Atividades do Evento
CREATE TABLE tb_activity_guests (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    event_guest_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_activity_guests_activity FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id),
    CONSTRAINT fk_activity_guests_event_guest FOREIGN KEY (event_guest_id) REFERENCES tb_event_guests (id) ON DELETE CASCADE,
    CONSTRAINT uk_activity_guest UNIQUE (activity_id, event_guest_id)
);

CREATE INDEX idx_activity_guests_created_at_id ON tb_activity_guests (created_at DESC, id DESC);
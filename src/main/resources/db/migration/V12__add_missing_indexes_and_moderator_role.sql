-- Permite persistir a nova função MODERATOR sem alterar o tipo textual da role.
ALTER TABLE tb_roles DROP CONSTRAINT IF EXISTS tb_roles_role_check;
ALTER TABLE tb_roles
    ADD CONSTRAINT ck_tb_roles_role
    CHECK ("role" IN ('ADMIN', 'MODERATOR', 'STAFF', 'USER'));

-- Índices ausentes em relacionamentos consultados por domínio e por proprietários.
CREATE INDEX idx_events_owner_id ON tb_events (owner_id);
CREATE INDEX idx_event_activities_event_order ON tb_event_activities (event_id, display_order);
CREATE INDEX idx_event_guests_event_id ON tb_event_guests (event_id);
CREATE INDEX idx_activity_guests_event_guest_id ON tb_activity_guests (event_guest_id);
CREATE INDEX idx_news_editors_news_user ON tb_news_editors (news_id, user_id);
CREATE INDEX idx_news_author_id ON tb_news (author_id);
CREATE INDEX idx_password_reset_token_user_id ON tb_password_reset_token (user_id);

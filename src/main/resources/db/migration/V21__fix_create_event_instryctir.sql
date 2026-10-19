ALTER TABLE tb_event_instructors
    DROP COLUMN IF EXISTS responded_at;

ALTER TABLE tb_event_instructor_invitations
    ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP;
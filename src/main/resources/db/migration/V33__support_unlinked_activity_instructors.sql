ALTER TABLE tb_event_instructors
    ALTER COLUMN user_id DROP NOT NULL,
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN description TEXT,
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'INSTRUCTOR'
        CHECK (role IN ('INSTRUCTOR', 'ASSISTANT'));

ALTER TABLE tb_activity_guests
    DROP CONSTRAINT IF EXISTS tb_activity_guests_role_check,
    ADD CONSTRAINT tb_activity_guests_role_check
        CHECK (role IN ('PRESENTER', 'ASSISTANT', 'SPECIAL_GUEST', 'MODERATOR', 'LISTENER'));

UPDATE tb_event_instructors instructor
SET name = COALESCE(NULLIF(users."name", ''), 'Ministrante')
FROM tb_users users
WHERE instructor.user_id = users.id;

UPDATE tb_event_instructors SET name = 'Ministrante' WHERE name IS NULL;
ALTER TABLE tb_event_instructors ALTER COLUMN name SET NOT NULL;

-- Support participant lookups, status filtering and cursor ordering.
CREATE INDEX idx_event_instructors_confirmed_activity_created
    ON tb_event_instructors (activity_id, created_at DESC, id DESC)
    WHERE status = 'CONFIRMED';

CREATE INDEX idx_activity_guests_activity_created
    ON tb_activity_guests (activity_id, created_at DESC, id DESC);

CREATE INDEX idx_event_guests_event_created
    ON tb_event_guests (event_id, created_at DESC, id DESC);

CREATE INDEX idx_instructor_invitations_activity_invited
    ON tb_event_instructor_invitations (activity_id, invited_at DESC, id DESC);

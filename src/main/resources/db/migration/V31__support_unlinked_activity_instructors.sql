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

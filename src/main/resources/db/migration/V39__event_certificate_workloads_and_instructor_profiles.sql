ALTER TABLE tb_events
    ADD COLUMN instructor_certificate_workload_hours NUMERIC,
    ADD COLUMN guest_certificate_workload_hours NUMERIC,
    ADD CONSTRAINT chk_events_instructor_certificate_workload_non_negative
        CHECK (instructor_certificate_workload_hours IS NULL OR instructor_certificate_workload_hours >= 0),
    ADD CONSTRAINT chk_events_guest_certificate_workload_non_negative
        CHECK (guest_certificate_workload_hours IS NULL OR guest_certificate_workload_hours >= 0);

CREATE TABLE tb_event_instructor_profiles (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES tb_events(id) ON DELETE CASCADE,
    user_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    legacy_instructor_id BIGINT
);

CREATE UNIQUE INDEX uk_event_instructor_profile_event_user
    ON tb_event_instructor_profiles(event_id, user_id)
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_event_instructor_profile_event_created
    ON tb_event_instructor_profiles(event_id, created_at DESC, id DESC);

ALTER TABLE tb_event_instructors ADD COLUMN profile_id BIGINT;

-- One shared event profile for a linked account, while unlinked legacy rows
-- remain separate because equal names do not prove equal identity.
INSERT INTO tb_event_instructor_profiles(event_id, user_id, name, description)
SELECT DISTINCT ON (activity.event_id, instructor.user_id)
       activity.event_id, instructor.user_id, instructor.name, instructor.description
FROM tb_event_instructors instructor
JOIN tb_event_activities activity ON activity.id = instructor.activity_id
WHERE instructor.user_id IS NOT NULL
ORDER BY activity.event_id, instructor.user_id, instructor.created_at, instructor.id;

INSERT INTO tb_event_instructor_profiles(event_id, user_id, name, description, legacy_instructor_id)
SELECT activity.event_id, NULL, instructor.name, instructor.description, instructor.id
FROM tb_event_instructors instructor
JOIN tb_event_activities activity ON activity.id = instructor.activity_id
WHERE instructor.user_id IS NULL;

UPDATE tb_event_instructors instructor
SET profile_id = profile.id
FROM tb_event_activities activity, tb_event_instructor_profiles profile
WHERE activity.id = instructor.activity_id
  AND profile.event_id = activity.event_id
  AND ((instructor.user_id IS NOT NULL AND profile.user_id = instructor.user_id)
       OR (instructor.user_id IS NULL AND profile.legacy_instructor_id = instructor.id));

UPDATE tb_event_instructors instructor
SET name = profile.name,
    description = profile.description
FROM tb_event_instructor_profiles profile
WHERE instructor.profile_id = profile.id;

ALTER TABLE tb_event_instructor_profiles DROP COLUMN legacy_instructor_id;
ALTER TABLE tb_event_instructors
    ADD CONSTRAINT fk_event_instructors_profile
        FOREIGN KEY (profile_id) REFERENCES tb_event_instructor_profiles(id) ON DELETE CASCADE;
CREATE INDEX idx_event_instructors_profile ON tb_event_instructors(profile_id);
CREATE UNIQUE INDEX uk_event_instructor_profile_activity
    ON tb_event_instructors(profile_id, activity_id)
    WHERE profile_id IS NOT NULL;

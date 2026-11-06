-- Event-owned rows are removed with the event, including dependencies that
-- previously had a foreign key but did not declare ON DELETE CASCADE.
ALTER TABLE tb_event_activities
    DROP CONSTRAINT fk_event_activities_event,
    ADD CONSTRAINT fk_event_activities_event
        FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE;

ALTER TABLE tb_event_editors
    DROP CONSTRAINT fk_event_editors_event,
    ADD CONSTRAINT fk_event_editors_event
        FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE;

ALTER TABLE tb_event_enrollments
    DROP CONSTRAINT fk_event_enrollments_event,
    ADD CONSTRAINT fk_event_enrollments_event
        FOREIGN KEY (events_id) REFERENCES tb_events (id) ON DELETE CASCADE;

ALTER TABLE tb_event_invitations
    DROP CONSTRAINT fk_event_invitations_event,
    ADD CONSTRAINT fk_event_invitations_event
        FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE;

ALTER TABLE tb_event_guests
    DROP CONSTRAINT fk_tb_event_guests_event,
    ADD CONSTRAINT fk_tb_event_guests_event
        FOREIGN KEY (event_id) REFERENCES tb_events (id) ON DELETE CASCADE;

ALTER TABLE tb_activity_guests
    DROP CONSTRAINT fk_activity_guests_activity,
    ADD CONSTRAINT fk_activity_guests_activity
        FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE;

ALTER TABLE tb_event_enrollment_activities
    DROP CONSTRAINT fk_enrollment_activity_activity,
    ADD CONSTRAINT fk_enrollment_activity_activity
        FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE;

-- These tables have event or activity IDs represented as scalar fields in the
-- JPA model. The foreign keys make the ownership explicit in the database too.
ALTER TABLE tb_events_check_in
    DROP CONSTRAINT fk_events_check_in_activity;
ALTER TABLE tb_events_check_in
    ADD CONSTRAINT fk_events_check_in_activity
        FOREIGN KEY (activity_id) REFERENCES tb_event_activities (id) ON DELETE CASCADE;

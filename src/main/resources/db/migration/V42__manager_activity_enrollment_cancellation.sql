ALTER TABLE tb_event_enrollment_activities
    ADD COLUMN canceled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN canceled_by UUID;

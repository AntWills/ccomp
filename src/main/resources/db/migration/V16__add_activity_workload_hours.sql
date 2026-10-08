ALTER TABLE tb_event_activities
    ADD COLUMN workload_hours NUMERIC,
    ADD CONSTRAINT chk_event_activity_workload_hours_non_negative
        CHECK (workload_hours IS NULL OR workload_hours >= 0);

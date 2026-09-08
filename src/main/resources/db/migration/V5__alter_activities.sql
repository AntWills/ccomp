ALTER TABLE tb_event_activities
    ADD COLUMN location VARCHAR(255),

    ADD COLUMN start_date TIMESTAMP(6),

    ADD COLUMN end_date TIMESTAMP(6),

    ADD COLUMN registration_requirement VARCHAR(25) NOT NULL
        CHECK (registration_requirement IN ('NOT_REQUIRED', 'REQUIRED')),

    ADD COLUMN access_policy VARCHAR(25) NOT NULL
        CHECK (access_policy IN ('EVENT_REGISTRANTS_ONLY', 'PUBLIC'));



ALTER TABLE tb_event_activities
    DROP CONSTRAINT tb_event_activities_registration_requirement_check,
    DROP CONSTRAINT tb_event_activities_access_policy_check;

ALTER TABLE tb_event_activities
    DROP COLUMN registration_requirement;

ALTER TABLE tb_event_activities
    RENAME COLUMN access_policy TO registration_policy;

ALTER TABLE tb_event_activities
    ADD CONSTRAINT tb_event_activities_registration_policy_check
        CHECK (registration_policy IN (
            'PUBLIC',
            'ACTIVITY_REGISTRANTS_ONLY',
            'EVENT_REGISTRANTS_ONLY',
            'INHERITED_FROM_EVENT'
        ));
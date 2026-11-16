ALTER TABLE tb_club_members
    DROP CONSTRAINT IF EXISTS ck_club_members_status;

ALTER TABLE tb_club_members
    ADD CONSTRAINT ck_club_members_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'BANNED', 'CANCELLED', 'INACTIVE'));

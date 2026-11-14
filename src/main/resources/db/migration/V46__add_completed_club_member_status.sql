ALTER TABLE tb_club_members
    DROP CONSTRAINT IF EXISTS tb_club_members_status_check;

ALTER TABLE tb_club_members
    ADD CONSTRAINT ck_club_members_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'INACTIVE'));

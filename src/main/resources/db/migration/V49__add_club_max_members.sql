ALTER TABLE tb_clubs
    ADD COLUMN max_members bigint,
    ADD CONSTRAINT ck_clubs_max_members_non_negative
        CHECK (max_members IS NULL OR max_members >= 0);

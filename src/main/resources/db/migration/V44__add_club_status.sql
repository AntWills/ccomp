ALTER TABLE tb_clubs
    ADD COLUMN status varchar(20);

UPDATE tb_clubs
SET status = CASE
    WHEN published_at IS NOT NULL AND published_at <= CURRENT_TIMESTAMP THEN 'OPEN'
    ELSE 'DRAFT'
END;

ALTER TABLE tb_clubs
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ALTER COLUMN status SET NOT NULL,
    ADD CONSTRAINT ck_clubs_status CHECK (status IN ('DRAFT', 'OPEN', 'MEMBERS_ONLY'));

CREATE INDEX idx_clubs_status_created_at_id
    ON tb_clubs (status, created_at DESC, id DESC);

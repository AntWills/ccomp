ALTER TABLE tb_users
    ALTER COLUMN "password" DROP NOT NULL,
    ADD COLUMN google_subject varchar(255);

CREATE UNIQUE INDEX uk_users_google_subject
    ON tb_users (google_subject)
    WHERE google_subject IS NOT NULL;

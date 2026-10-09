ALTER TABLE tb_roles
    DROP CONSTRAINT fk_roles_user;

ALTER TABLE tb_roles
    ADD CONSTRAINT fk_roles_user
        FOREIGN KEY (user_id)
        REFERENCES tb_users (id)
        ON DELETE CASCADE;
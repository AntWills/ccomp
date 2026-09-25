CREATE TABLE tb_storage_role_permissions (
    role varchar(25) PRIMARY KEY CHECK (role IN ('ADMIN', 'MODERATOR', 'STAFF', 'USER')),
    can_upload boolean NOT NULL DEFAULT false,
    can_read_own boolean NOT NULL DEFAULT false,
    can_read_any boolean NOT NULL DEFAULT false,
    can_delete_own boolean NOT NULL DEFAULT false,
    can_delete_any boolean NOT NULL DEFAULT false
);

INSERT INTO tb_storage_role_permissions
    (role, can_upload, can_read_own, can_read_any, can_delete_own, can_delete_any)
VALUES
    ('USER', false, false, false, false, false),
    ('STAFF', true, true, false, true, false),
    ('MODERATOR', true, true, true, true, true),
    ('ADMIN', true, true, true, true, true);

ALTER TABLE tb_event_materials DROP CONSTRAINT tb_event_materials_type_check;
ALTER TABLE tb_event_materials ADD CONSTRAINT ck_event_material_type
    CHECK (type IN ('LINK', 'FILE', 'IMAGE'));
ALTER TABLE tb_event_materials DROP CONSTRAINT ck_event_material_target;
ALTER TABLE tb_event_materials ADD CONSTRAINT ck_event_material_target CHECK (
    (type = 'LINK' AND url IS NOT NULL AND file_key IS NULL AND original_file_name IS NULL AND content_type IS NULL AND size_bytes IS NULL)
    OR
    (type IN ('FILE', 'IMAGE') AND url IS NULL AND file_key IS NOT NULL AND original_file_name IS NOT NULL AND content_type IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
);

ALTER TABLE tb_activity_materials DROP CONSTRAINT tb_activity_materials_type_check;
ALTER TABLE tb_activity_materials ADD CONSTRAINT ck_activity_material_type
    CHECK (type IN ('LINK', 'FILE', 'IMAGE'));
ALTER TABLE tb_activity_materials DROP CONSTRAINT ck_activity_material_target;
ALTER TABLE tb_activity_materials ADD CONSTRAINT ck_activity_material_target CHECK (
    (type = 'LINK' AND url IS NOT NULL AND file_key IS NULL AND original_file_name IS NULL AND content_type IS NULL AND size_bytes IS NULL)
    OR
    (type IN ('FILE', 'IMAGE') AND url IS NULL AND file_key IS NOT NULL AND original_file_name IS NOT NULL AND content_type IS NOT NULL AND size_bytes IS NOT NULL AND size_bytes > 0)
);

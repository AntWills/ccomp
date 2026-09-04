ALTER TABLE tb_event_editor_validations
    RENAME TO tb_event_editor_invitations;

ALTER TABLE tb_event_editor_invitations
	ADD COLUMN email_address VARCHAR(255);
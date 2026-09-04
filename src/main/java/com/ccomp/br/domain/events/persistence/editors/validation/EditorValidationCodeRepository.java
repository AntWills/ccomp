package com.ccomp.br.domain.events.persistence.editors.validation;

import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EditorValidationCodeRepository extends JpaRepository<EventEditorInvitations, Long> {
    Optional<EventEditorInvitations> findByCode(UUID code);

    void deleteByEventEditor(EventEditor eventEditor);
}

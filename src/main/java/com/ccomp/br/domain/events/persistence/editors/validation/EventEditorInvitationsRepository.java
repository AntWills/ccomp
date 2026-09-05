package com.ccomp.br.domain.events.persistence.editors.validation;

import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.module.email.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EventEditorInvitationsRepository extends JpaRepository<EventEditorInvitations, Long> {
    Optional<EventEditorInvitations> findByCode(UUID code);
    Optional<EventEditorInvitations> findByEmailAddressAndEventId(EmailAddress emailAddress, Long eventId);
    boolean existsByEmailAddressAndEventId(EmailAddress emailAddress, Long eventId);
    void deleteByEmailAddress(EmailAddress emailAddress);
}

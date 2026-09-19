package com.ccomp.br.domain.events.guests.persistence.invitations;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.module.email.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, Long> {
    Optional<EventInvitation> findByEmailAddressAndEvent(EmailAddress emailAddress, Event event);
    Optional<EventInvitation> findByCode(UUID code);
}
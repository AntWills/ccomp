package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventGuestRepository extends JpaRepository<EventGuest, Long> {
    boolean existsByUserIdAndEvent(UUID userId, Event event);

    List<EventGuest> findByEventId(Long eventId);

    List<EventGuest> findByEventIdAndVisibilityAndStatus(
            Long eventId, EnumGuestVisibility visibility, EnumGuestStatus status);

    Optional<EventGuest> findByIdAndEventId(Long id, Long eventId);
}
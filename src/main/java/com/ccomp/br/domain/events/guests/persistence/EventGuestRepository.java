package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.core.persistence.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventGuestRepository extends JpaRepository<EventGuest, Long> {
    boolean existsByUserIdAndEvent(UUID userId, Event event);
}
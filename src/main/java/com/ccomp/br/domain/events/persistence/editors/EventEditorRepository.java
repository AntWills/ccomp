package com.ccomp.br.domain.events.persistence.editors;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventEditorRepository extends JpaRepository<EventEditor, Long> {
    boolean existsByEventIdAndUserId(Long eventId, UUID uuid);

    void deleteByEventIdAndUserId(Long eventId, UUID userId);
}
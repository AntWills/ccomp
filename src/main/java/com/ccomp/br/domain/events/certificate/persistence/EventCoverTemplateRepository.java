package com.ccomp.br.domain.events.certificate.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventCoverTemplateRepository extends JpaRepository<EventCoverTemplate, Long> {
    Optional<EventCoverTemplate> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}
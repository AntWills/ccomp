package com.ccomp.br.domain.events.certificate.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventActivityBoxTemplateRepository extends JpaRepository<EventActivityBoxTemplate, Long> {
    Optional<EventActivityBoxTemplate> findByEventId(Long eventId);
}

package com.ccomp.br.domain.events.certificate.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventBoxTemplateRepository extends JpaRepository<EventBoxTemplate, Long> {
    Optional<EventBoxTemplate> findByEventId(Long eventId);
}
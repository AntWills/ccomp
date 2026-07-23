package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOwnerId(UUID ownerId);
    Optional<Event> findBySlug(String slug);
}
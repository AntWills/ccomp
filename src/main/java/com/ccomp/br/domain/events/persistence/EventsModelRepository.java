package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventsModelRepository extends JpaRepository<EventsModel, Long> {
    List<EventsModel> findAllByOwnerId(UUID ownerId);
}
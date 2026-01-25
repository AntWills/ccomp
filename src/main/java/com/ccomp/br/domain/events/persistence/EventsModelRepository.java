package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventsModelRepository extends JpaRepository<EventsModel, Long> {
}
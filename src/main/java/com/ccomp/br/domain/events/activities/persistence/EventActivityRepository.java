package com.ccomp.br.domain.events.activities.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventActivityRepository extends JpaRepository<EventActivity, Long> {
}
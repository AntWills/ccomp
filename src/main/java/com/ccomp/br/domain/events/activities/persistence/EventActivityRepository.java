package com.ccomp.br.domain.events.activities.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventActivityRepository extends JpaRepository<EventActivity, Long> {
    List<EventActivity> findAllByEventId(Long eventId);
}

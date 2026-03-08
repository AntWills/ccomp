package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.persistence.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventActivityRepository extends JpaRepository<EventActivity, Long> {
}
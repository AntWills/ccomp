package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEditorRepository extends JpaRepository<EventEditor, Long> {
}
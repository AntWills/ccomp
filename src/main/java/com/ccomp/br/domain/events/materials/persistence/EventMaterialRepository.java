package com.ccomp.br.domain.events.materials.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventMaterialRepository extends JpaRepository<EventMaterial, Long> {
    List<EventMaterial> findAllByEventId(Long eventId);
    Optional<EventMaterial> findByIdAndEventId(Long id, Long eventId);
}

package com.ccomp.br.domain.events.instructor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventInstructorProfileRepository extends JpaRepository<EventInstructorProfile, Long> {
    List<EventInstructorProfile> findAllByEventIdOrderByCreatedAtDescIdDesc(Long eventId);
    Optional<EventInstructorProfile> findByIdAndEventId(Long id, Long eventId);
    Optional<EventInstructorProfile> findByEventIdAndUserId(Long eventId, UUID userId);
}

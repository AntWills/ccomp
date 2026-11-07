package com.ccomp.br.domain.events.instructor.persistence;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventInstructorRepository extends JpaRepository<EventInstructor, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, UUID userId);
    Optional<EventInstructor> findByActivityIdAndUserId(Long activityId, UUID userId);
    Optional<EventInstructor> findByIdAndActivityId(Long id, Long activityId);
}

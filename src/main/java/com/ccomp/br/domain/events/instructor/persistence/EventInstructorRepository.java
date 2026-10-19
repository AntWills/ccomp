package com.ccomp.br.domain.events.instructor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface EventInstructorRepository extends JpaRepository<EventInstructor, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, UUID userId);
    Optional<EventInstructor> findByActivityIdAndUserId(Long activityId, UUID userId);
}

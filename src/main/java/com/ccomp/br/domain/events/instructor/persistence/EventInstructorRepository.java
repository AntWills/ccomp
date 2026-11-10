package com.ccomp.br.domain.events.instructor.persistence;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventInstructorRepository extends JpaRepository<EventInstructor, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, UUID userId);
    Optional<EventInstructor> findByActivityIdAndUserId(Long activityId, UUID userId);
    Optional<EventInstructor> findByIdAndActivityId(Long id, Long activityId);
    List<EventInstructor> findAllByProfile_Id(Long profileId);
    Optional<EventInstructor> findByActivityIdAndProfile_Id(Long activityId, Long profileId);
}

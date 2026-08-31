package com.ccomp.br.domain.events.persistence.enrollments;

import com.ccomp.br.domain.events.persistence.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.event WHERE e.userId = :userId")
    List<Enrollment> findAllByUserIdWithEvent(@Param("userId") UUID userId);

    Optional<Enrollment> findByUserIdAndEvent(UUID userId, Event event);

    boolean existsByUserIdAndEvent(UUID userId, Event event);
    void deleteByUserIdAndEvent(UUID userId, Event event);
}
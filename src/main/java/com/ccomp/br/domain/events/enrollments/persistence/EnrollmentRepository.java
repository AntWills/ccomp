package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.event WHERE e.userId = :userId")
    List<Enrollment> findAllByUserIdWithEvent(@Param("userId") UUID userId);

    Optional<Enrollment> findByUserIdAndEvent(UUID userId, Event event);

    @Query("select e from Enrollment e where e.userId = :userId and e.event.id = :eventId")
    Optional<Enrollment> findByUserIdAndEventId(@Param("userId") UUID userId, @Param("eventId") Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Enrollment> findByIdAndEventId(Long id, Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.userId = :userId and e.event.id = :eventId")
    Optional<Enrollment> findForCredentialByUserIdAndEventId(
            @Param("userId") UUID userId, @Param("eventId") Long eventId);

    long countByEventIdAndStatusIn(Long eventId,
            Collection<EnumEnrollmentState> statuses);

    boolean existsByUserIdAndEvent(UUID userId, Event event);
    void deleteByUserIdAndEvent(UUID userId, Event event);
}

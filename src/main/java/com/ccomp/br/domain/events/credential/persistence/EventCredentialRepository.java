package com.ccomp.br.domain.events.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventCredentialRepository extends JpaRepository<EventCredential, Long> {
    Optional<EventCredential> findByEventIdAndEnrollmentId(Long eventId, Long enrollmentId);
    Optional<EventCredential> findByEventIdAndTokenHash(Long eventId, String tokenHash);
    @Query("select c.enrollmentId from EventCredential c where c.eventId = :eventId and c.tokenHash = :tokenHash")
    Optional<Long> findEnrollmentIdByEventIdAndTokenHash(@Param("eventId") Long eventId,
                                                          @Param("tokenHash") String tokenHash);
    boolean existsByEventIdAndEnrollmentId(Long eventId, Long enrollmentId);
    List<EventCredential> findAllByEventId(Long eventId);
    long countByEventIdAndCredentialedAtIsNotNull(Long eventId);
}

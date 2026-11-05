package com.ccomp.br.domain.events.credential.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventCredentialOperatorRepository extends JpaRepository<EventCredentialOperator, Long> {
    boolean existsByEventIdAndUserId(Long eventId, UUID userId);
    Optional<EventCredentialOperator> findByEventIdAndUserId(Long eventId, UUID userId);
}

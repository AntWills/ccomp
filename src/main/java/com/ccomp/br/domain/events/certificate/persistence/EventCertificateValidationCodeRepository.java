package com.ccomp.br.domain.events.certificate.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventCertificateValidationCodeRepository extends JpaRepository<EventCertificateValidationCode, Long> {

    Optional<EventCertificateValidationCode> findByCode(String code);

    // Busca apenas o certificado GERAL do evento (onde activity_id é NULL)
    Optional<EventCertificateValidationCode> findByEventIdAndActivityIdIsNullAndUserId(Long eventId, UUID userId);

    // Busca o certificado específico de uma ATIVIDADE
    Optional<EventCertificateValidationCode> findByActivityIdAndUserId(Long activityId, UUID userId);

    boolean existsByCode(String code);
}
package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventCertificateValidationCodeRepository extends JpaRepository<EventCertificateValidationCode, Long> {

    Optional<EventCertificateValidationCode> findByCode(String code);

    // Distingue o certificado de participante, ministrante ou equipe do evento.
    Optional<EventCertificateValidationCode> findByEventIdAndActivityIdIsNullAndUserIdAndCertificateType(
            Long eventId, UUID userId, EnumCertificateType certificateType);

    // Busca o certificado específico de uma atividade.
    Optional<EventCertificateValidationCode> findByActivityIdAndUserIdAndCertificateType(
            Long activityId, UUID userId, EnumCertificateType certificateType);

    boolean existsByCode(String code);
}

package com.ccomp.br.domain.events.certificate.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;

public interface EventActivityBoxTemplateRepository extends JpaRepository<EventActivityBoxTemplate, Long> {
    Optional<EventActivityBoxTemplate> findByEventId(Long eventId);
    Optional<EventActivityBoxTemplate> findByEventIdAndTemplateType(Long eventId, EnumActivityCertificateTemplateType templateType);
}

package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/** Centraliza a regra de carga usada na emissão e na validação pública. */
@Service
public class CertificateWorkloadService {
    private final EventActivityDslRepository activityRepository;

    public CertificateWorkloadService(EventActivityDslRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public BigDecimal forEvent(EventDTO event, UUID userId, EnumCertificateType type) {
        BigDecimal configured = switch (type) {
            case EVENT -> event.workloadHours();
            case INSTRUCTOR -> event.instructorCertificateWorkloadHours();
            case GUEST -> event.guestCertificateWorkloadHours();
            case EDITOR -> event.teamCertificateWorkloadHours();
            case ACTIVITY -> BigDecimal.ZERO;
        };
        BigDecimal earned = switch (type) {
            case EVENT -> activityRepository.sumWorkloadHoursByCheckIn(userId, event.id());
            case INSTRUCTOR -> activityRepository.sumWorkloadHoursByInstructor(userId, event.id());
            case GUEST -> activityRepository.sumWorkloadHoursByGuest(userId, event.id());
            case EDITOR, ACTIVITY -> BigDecimal.ZERO;
        };
        return add(configured, earned);
    }

    public BigDecimal forActivity(BigDecimal workload) {
        return workload == null ? BigDecimal.ZERO : workload;
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        return (first == null ? BigDecimal.ZERO : first).add(second == null ? BigDecimal.ZERO : second);
    }
}

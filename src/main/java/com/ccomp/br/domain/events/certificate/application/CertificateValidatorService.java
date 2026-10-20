package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCodeRepository;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CertificateValidatorService {
    private static final String CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final EventCertificateValidationCodeRepository validationCodeRepository;
    private final EventCache eventCache;
    private final ActivityCache activityCache;
    private final EventActivityDslRepository activityDslRepository;
    private final UserManagement userManagement;

    public CertificateValidatorService(EventCertificateValidationCodeRepository validationCodeRepository,
                                       EventCache eventCache,
                                       ActivityCache activityCache,
                                       EventActivityDslRepository activityDslRepository,
                                       UserManagement userManagement) {
        this.validationCodeRepository = validationCodeRepository;
        this.eventCache = eventCache;
        this.activityCache = activityCache;
        this.activityDslRepository = activityDslRepository;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CertificateValidationResponse validate(String code) {
        EventCertificateValidationCode validationCode = validationCodeRepository.findByCode(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado para o código informado."));

        var userDto = userManagement.findById(validationCode.getUserId())
                .orElseThrow(() -> new DomainException("Usuário não enconstrado."));

        var eventDto = eventCache.findById(validationCode.getEventId())
                .orElseThrow(() -> new DomainException("O evento vinculado não existe."));

        if (validationCode.getActivityId() != null) {
            var activity = activityCache.findById(validationCode.getActivityId())
                    .orElseThrow(() -> new DomainException("A atividade vinculada não existe."));

            return new CertificateValidationResponse(validationCode.getCode(),
                    validationCode.getUserId(),
                    userDto.name(),
                    eventDto.title(),
                    activity.title(),
                    formatWorkloadHours(activity.workloadHours()),
                    validationCode.getCreatedAt());
        }

        return new CertificateValidationResponse(
                validationCode.getCode(),
                validationCode.getUserId(),
                userDto.name(),
                eventDto.title(),
                null,
                formatWorkloadHours(activityDslRepository.sumWorkloadHoursByCheckIn(userDto.id(), eventDto.id())),
                validationCode.getCreatedAt()
        );
    }

    /** Aceita o código com espaços ou em minúsculas (ex.: digitado manualmente). */
    private String normalize(String code) {
        if (code == null) {
            throw new ResourceNotFoundException("Certificado não encontrado para o código informado.");
        }

        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new ResourceNotFoundException("Certificado não encontrado para o código informado.");
        }
        return normalized;
    }

    private String formatWorkloadHours(BigDecimal workloadHours) {
        if (workloadHours == null) {
            return "0 horas";
        }
        return workloadHours.stripTrailingZeros().toPlainString() + " horas";
    }

    /**
     * Emite ou recupera o código de validação do certificado GERAL do evento.
     */
    @Transactional
    public EventCertificateValidationCode issueForEvent(Long eventId, UUID userId) {
        return validationCodeRepository.findByEventIdAndActivityIdIsNullAndUserId(eventId, userId)
                .orElseGet(() -> createAndSaveValidationCode(eventId, null, userId));
    }

    @Transactional
    public EventCertificateValidationCode issueForActivity(Long eventId, Long activityId, UUID userId) {
        return validationCodeRepository.findByActivityIdAndUserId(activityId, userId)
                .orElseGet(() -> createAndSaveValidationCode(eventId, activityId, userId));
    }

    private EventCertificateValidationCode createAndSaveValidationCode(Long eventId, Long activityId, UUID userId) {
        EventCertificateValidationCode validationCode = EventCertificateValidationCode.builder()
                .code(generateUniqueCode())
                .eventId(eventId)
                .activityId(activityId)
                .userId(userId)
                .build();

        return validationCodeRepository.save(validationCode);
    }

    private String generateUniqueCode() {
        for (int i = 1; i <= MAX_GENERATION_ATTEMPTS; i++) {
            String code = RandomStringUtils.secure().next(CODE_LENGTH, CODE_ALPHABET);
            if (!validationCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new DomainException("Não foi possível gerar um código de validação único.");
    }
}

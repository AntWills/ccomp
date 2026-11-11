package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCodeRepository;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CertificateWorkloadService workloadService;

    @Autowired
    public CertificateValidatorService(EventCertificateValidationCodeRepository validationCodeRepository,
                                       EventCache eventCache,
                                       ActivityCache activityCache,
                                       EventActivityDslRepository activityDslRepository,
                                       UserManagement userManagement,
                                       CertificateWorkloadService workloadService) {
        this.validationCodeRepository = validationCodeRepository;
        this.eventCache = eventCache;
        this.activityCache = activityCache;
        this.activityDslRepository = activityDslRepository;
        this.userManagement = userManagement;
        this.workloadService = workloadService;
    }

    public CertificateValidatorService(EventCertificateValidationCodeRepository validationCodeRepository,
                                       EventCache eventCache,
                                       ActivityCache activityCache,
                                       EventActivityDslRepository activityDslRepository,
                                       UserManagement userManagement) {
        this(validationCodeRepository, eventCache, activityCache, activityDslRepository, userManagement,
                new CertificateWorkloadService(activityDslRepository));
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
                    validationCode.getCreatedAt(), validationCode.getCertificateType());
        }

        BigDecimal workload = switch (validationCode.getCertificateType()) {
            case EVENT, INSTRUCTOR, GUEST, EDITOR -> workloadService.forEvent(
                    eventDto, userDto.id(), validationCode.getCertificateType());
            case ACTIVITY -> BigDecimal.ZERO;
        };

        return new CertificateValidationResponse(
                validationCode.getCode(),
                validationCode.getUserId(),
                userDto.name(),
                eventDto.title(),
                null,
                formatWorkloadHours(workload),
                validationCode.getCreatedAt(),
                validationCode.getCertificateType()
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
        return issueForEvent(eventId, userId, EnumCertificateType.EVENT);
    }

    @Transactional
    public EventCertificateValidationCode issueForEvent(Long eventId, UUID userId, EnumCertificateType type) {
        return validationCodeRepository.findByEventIdAndActivityIdIsNullAndUserIdAndCertificateType(eventId, userId, type)
                .orElseGet(() -> createAndSaveValidationCode(eventId, null, userId, type));
    }

    @Transactional
    public EventCertificateValidationCode issueForActivity(Long eventId, Long activityId, UUID userId) {
        return issueForActivity(eventId, activityId, userId, EnumCertificateType.ACTIVITY);
    }

    @Transactional
    public EventCertificateValidationCode issueForActivity(Long eventId, Long activityId, UUID userId,
                                                            EnumCertificateType type) {
        return validationCodeRepository.findByActivityIdAndUserIdAndCertificateType(activityId, userId, type)
                .orElseGet(() -> createAndSaveValidationCode(eventId, activityId, userId, type));
    }

    private EventCertificateValidationCode createAndSaveValidationCode(Long eventId, Long activityId, UUID userId,
                                                                       EnumCertificateType type) {
        EventCertificateValidationCode validationCode = EventCertificateValidationCode.builder()
                .code(generateUniqueCode())
                .eventId(eventId)
                .activityId(activityId)
                .certificateType(type)
                .userId(userId)
                .build();

        return validationCodeRepository.save(validationCode);
    }

    private BigDecimal addHours(BigDecimal first, BigDecimal second) {
        return (first == null ? BigDecimal.ZERO : first).add(second == null ? BigDecimal.ZERO : second);
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

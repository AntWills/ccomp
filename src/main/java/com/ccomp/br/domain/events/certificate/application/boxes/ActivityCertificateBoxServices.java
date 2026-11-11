package com.ccomp.br.domain.events.certificate.application.boxes;

import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateTemplateVariableExtractor;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateVariableContext;
import com.ccomp.br.domain.events.certificate.persistence.ActivityCertificateTemplate;
import com.ccomp.br.domain.events.certificate.persistence.ActivityCertificateTemplateRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplateRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.certificate.application.ResolvedActivityCertificateTemplate;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityCertificateBoxServices {
    private static final int MAX_BOXES = 30;

    private final EventActivityBoxTemplateRepository templateRepository;
    private final CertificateFetcher certificateFetcher;
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final ActivityCertificateTemplateRepository activityTemplateRepository;
    private final EventActivityRepository activityRepository;

    public ActivityCertificateBoxServices(EventActivityBoxTemplateRepository templateRepository,
                                          CertificateFetcher certificateFetcher,
                                          EventCache eventCache,
                                          EventAccessPolicy accessPolicy,
                                          ActivityCertificateTemplateRepository activityTemplateRepository,
                                          EventActivityRepository activityRepository) {
        this.templateRepository = templateRepository;
        this.certificateFetcher = certificateFetcher;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.activityTemplateRepository = activityTemplateRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public EventActivityBoxTemplate loadBoxTemplate(long eventId, UUID userId) {
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canView(event, userId)) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }
        return certificateFetcher.fetchActivityBoxTemplate(eventId);
    }

    @Transactional
    public void saveBoxTemplate(long eventId, UUID userId, List<BoxElementDTO> boxes) {
        saveBoxTemplate(eventId, userId, boxes, EnumActivityCertificateTemplateType.PARTICIPANT);
    }

    @Transactional(readOnly = true)
    public EventActivityBoxTemplate loadBoxTemplate(long eventId, UUID userId,
                                                   EnumActivityCertificateTemplateType context) {
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canView(event, userId)) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }
        return certificateFetcher.fetchActivityBoxTemplate(eventId, context);
    }

    @Transactional
    public void saveBoxTemplate(long eventId, UUID userId, List<BoxElementDTO> boxes,
                                EnumActivityCertificateTemplateType context) {
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        if (boxes == null || boxes.isEmpty()) {
            throw new DomainException("A lista de caixas não pode estar vazia.");
        }
        if (boxes.size() > MAX_BOXES) {
            throw new DomainException("O template aceita no máximo 30 caixas de texto.");
        }

        EventActivityBoxTemplate template = templateRepository.findByEventIdAndTemplateType(eventId, context)
                .orElseGet(() -> EventActivityBoxTemplate.builder().eventId(eventId)
                        .templateType(context).build());
        template.setBoxes(List.copyOf(boxes));
        template.setVariables(CertificateTemplateVariableExtractor.extract(boxes, variableContext(context)));
        templateRepository.save(template);
    }

    @Transactional
    public ResolvedActivityCertificateTemplate loadActivityTemplate(long eventId, long activityId, UUID userId,
                                                                    EnumActivityCertificateTemplateType context) {
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        requireActivity(eventId, activityId);
        return certificateFetcher.resolveActivityTemplate(eventId, activityId, context);
    }

    @Transactional
    public void saveActivityTemplate(long eventId, long activityId, UUID userId,
                                     List<BoxElementDTO> boxes,
                                     EnumActivityCertificateTemplateType context) {
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        requireActivity(eventId, activityId);
        validateBoxes(boxes);
        ActivityCertificateTemplate template = activityTemplateRepository
                .findByActivityIdAndTemplateType(activityId, context)
                .orElseGet(() -> ActivityCertificateTemplate.builder()
                        .activityId(activityId).templateType(context).build());
        template.setTemplateType(context);
        template.setBoxes(List.copyOf(boxes));
        template.setVariables(CertificateTemplateVariableExtractor.extract(boxes, variableContext(context)));
        activityTemplateRepository.save(template);
    }

    private void validateBoxes(List<BoxElementDTO> boxes) {
        if (boxes == null || boxes.isEmpty()) throw new DomainException("A lista de caixas não pode estar vazia.");
        if (boxes.size() > MAX_BOXES) throw new DomainException("O template aceita no máximo 30 caixas de texto.");
    }

    private EnumCertificateVariableContext variableContext(EnumActivityCertificateTemplateType context) {
        return switch (context) {
            case PARTICIPANT -> EnumCertificateVariableContext.ACTIVITY;
            case INSTRUCTOR -> EnumCertificateVariableContext.ACTIVITY_INSTRUCTOR;
            case GUEST -> EnumCertificateVariableContext.ACTIVITY_GUEST;
        };
    }

    private EventActivity requireActivity(long eventId, long activityId) {
        return activityRepository.findById(activityId)
                .filter(activity -> activity.getEventId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
    }

    private EventDTO findEvent(long eventId) {
        return eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }
}

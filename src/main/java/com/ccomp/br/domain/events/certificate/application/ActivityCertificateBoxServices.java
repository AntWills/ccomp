package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplateRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
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

    public ActivityCertificateBoxServices(EventActivityBoxTemplateRepository templateRepository,
                                          CertificateFetcher certificateFetcher,
                                          EventCache eventCache,
                                          EventAccessPolicy accessPolicy) {
        this.templateRepository = templateRepository;
        this.certificateFetcher = certificateFetcher;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
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
        EventDTO event = findEvent(eventId);
        if (!accessPolicy.canEdit(event, userId)) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }
        if (boxes == null || boxes.isEmpty()) {
            throw new DomainException("A lista de caixas não pode estar vazia.");
        }
        if (boxes.size() > MAX_BOXES) {
            throw new DomainException("O template aceita no máximo 30 caixas de texto.");
        }

        EventActivityBoxTemplate template = templateRepository.findByEventId(eventId)
                .orElseGet(() -> EventActivityBoxTemplate.builder().eventId(eventId).build());
        template.setBoxes(List.copyOf(boxes));
        template.setVariables(CertificateTemplateVariableExtractor.extract(boxes));
        templateRepository.save(template);
    }

    private EventDTO findEvent(long eventId) {
        return eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }
}

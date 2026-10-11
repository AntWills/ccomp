package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplateRepository;
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
public class CertificateBoxServices {
    private static final int MAX_BOXES = 30;

    private final EventBoxTemplateRepository boxTemplateRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;

    public CertificateBoxServices(EventBoxTemplateRepository boxTemplateRepository, EventCache eventCache,
                                  EventAccessPolicy accessPolicy) {
        this.boxTemplateRepository = boxTemplateRepository;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<BoxElementDTO> loadBoxTemplate(long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        return boxTemplateRepository.findByEventId(eventId)
                .map(EventBoxTemplate::getBoxes)
                .orElseGet(List::of);
    }

    @Transactional
    public void saveBoxTemplate(long eventId, UUID userId, List<BoxElementDTO> boxes) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        if (boxes == null || boxes.isEmpty())
            throw new DomainException("A lista de caixas não pode estar vazia.");
        if (boxes.size() > MAX_BOXES)
            throw new DomainException("O template aceita no máximo 30 caixas de texto.");

        EventBoxTemplate template = boxTemplateRepository.findByEventId(eventId)
                .orElseGet(() -> EventBoxTemplate.builder().eventId(eventId).build());
        template.setBoxes(List.copyOf(boxes));
        boxTemplateRepository.save(template);
    }
}

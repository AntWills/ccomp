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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificateBoxServices {
    private static final int MAX_BOXES = 30;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^{}]+}");

    private final EventBoxTemplateRepository boxTemplateRepository;
    private final CertificateFetcher certificateFetcher;
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;

    public CertificateBoxServices(EventBoxTemplateRepository boxTemplateRepository,
                                  CertificateFetcher certificateFetcher,
                                  EventCache eventCache,
                                  EventAccessPolicy accessPolicy) {
        this.boxTemplateRepository = boxTemplateRepository;
        this.certificateFetcher = certificateFetcher;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public EventBoxTemplate loadBoxTemplate(long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        return certificateFetcher.fetchBoxTemplate(eventId);
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
        template.setVariables(extractVariables(boxes));
        boxTemplateRepository.save(template);
    }

    private List<String> extractVariables(List<BoxElementDTO> boxes) {
        Set<String> allowedVariables = Set.copyOf(CertificateVariablesServices.VARIABLES);
        Set<String> extractedVariables = new LinkedHashSet<>();

        for (BoxElementDTO box : boxes) {
            Matcher matcher = VARIABLE_PATTERN.matcher(box.textTemplate());
            while (matcher.find()) {
                String variable = matcher.group();
                if (!allowedVariables.contains(variable))
                    throw new DomainException("Variável não suportada no template: " + variable);
                extractedVariables.add(variable);
            }
        }

        return List.copyOf(extractedVariables);
    }
}

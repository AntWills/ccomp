package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.events.*;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.persistence.EventBlaze;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.news.util.SlugUtils;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.EventListItemView;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.DebugUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventsServices {
    private final int MAX_PAGE_SIZE = 50;
    private final EventRepository eventRepository;
    private final UserManagement userManagement;
    private final EventMapper eventMapper;
    private final EditorServices editorServices;
    private final EventBlaze eventBlaze;

    public EventsServices(EventRepository eventRepository, UserManagement userManagement, EventMapper eventMapper, EditorServices editorServices, EventBlaze eventBlaze) {
        this.eventRepository = eventRepository;
        this.userManagement = userManagement;
        this.eventMapper = eventMapper;
        this.editorServices = editorServices;
        this.eventBlaze = eventBlaze;
    }

    // ---- Consultas ----
    @Transactional(readOnly = true)
    public Optional<EventDTO> getById(Long eventId, UUID userId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    // O evento pode ser acessado se estiver publicado/unlisted OU se o usuário for dono/editor/admin
                    boolean allowed = event.isPubliclyAccessible()
                            || (userId != null && event.isOwner(userId))
                            || (userId != null && editorServices.hasPermissionEdit(event, userId))
                            || SecurityUtils.isAdmin();

                    if (allowed) return eventMapper.eventToEventDTO(event);

                    throw new AccessDeniedException("Você não possui permissão para visualizar este evento.");
                });
    }

    @Transactional(readOnly = true)
    public Optional<EventDTO> getBySlug(String slug) {
        // A busca direta por slug público exige obrigatoriamente que o status seja PUBLISHED
        return eventRepository.findBySlug(slug)
                .filter(Event::isPublished)
                .map(eventMapper::eventToEventDTO);
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemView> searchEventsWithFilters(
            EventsFilterRequest filter, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class).orElse(null);
        List<EventListItemView> events = eventBlaze.findByCursor(filter, decodedCursor, finalPageSize + 1);

        log.info("Quantidade de eventos retornados: {}", events.size());
        log.info("Horário da consulta: {}", LocalDateTime.now());

        return CursorUtils.buildPage(events, finalPageSize, e -> new EventCursor(e.getStartDate(), e.getId()));
    }

    // ---- Comandos ----
    @Transactional
    public EventDTO create(UUID ownerId, CreateEventDTO dto) {
        UserDTO userDTO = userManagement.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Usuário responsável não encontrado no sistema."));

        if (!userDTO.isTeamMember())
            throw new AccessDeniedException("Apenas membros da equipe (STAFF, MODERATOR ou ADMIN) podem criar novos eventos.");


        var eventModel = Event.builder()
                .title(dto.title())
                .slug(generateSlug(dto.title()))
                .summary("Exemplo de sumário")
                .category(dto.category())
                .format(dto.format())
                .status(EnumEventStatus.DRAFT) // Todo evento nasce como rascunho por padrão
                .ownerId(ownerId)
                .build();

        log.info("Registrando novo evento: {}", DebugUtils.printJson(eventModel));

        dto.optionalStartDate().ifPresent(eventModel::setStartDate);
        dto.optionalEndDate().ifPresent(eventModel::setEndDate);

        var savedEvent = eventRepository.save(eventModel);

        return eventMapper.eventToEventDTO(savedEvent);
    }

    private String generateSlug(String title) {
        String base = SlugUtils.toSlug(title);

        while (true) {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            String slug = base + "-" + suffix;

            if (!eventRepository.existsBySlug(slug)) {
                return slug;
            }
        }
    }

    @Transactional
    public EventDTO update(UpdateEventDTO request, Long eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean canEdit = SecurityUtils.isAdmin()
                || event.isOwner(userId)
                || editorServices.hasPermissionEdit(event, userId);

        if (!canEdit)
            throw new AccessDeniedException("Você não tem permissão para alterar o status deste evento.");


        request.titleOpt().ifPresent(title -> {
            event.setTitle(title);
            event.setSlug(generateSlug(title));
        });

        eventMapper.updateEntityFromDto(request, event);

        eventRepository.save(event);

        return eventMapper.eventToEventDTO(event);
    }

    @Transactional
    public MessageResponse updateEventStatus(Long eventId, EnumEventStatus newStatus, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean canEdit = SecurityUtils.isAdmin()
                || event.isOwner(userId)
                || editorServices.hasPermissionEdit(event, userId);

        if (!canEdit) {
            throw new AccessDeniedException("Você não tem permissão para alterar o status deste evento.");
        }

        // Executa a transição através dos métodos de domínio encapsulados
        switch (newStatus) {
            case PUBLISHED -> event.publish();
            case CANCELED -> event.cancel();
            case DRAFT -> event.moveToDraft();
            case UNLISTED -> event.unlist();
        }

        eventRepository.save(event);

        return new MessageResponse("Status do evento alterado para: " + newStatus.name());
    }

    @Transactional
    public void delete(Long eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean canEdit = SecurityUtils.isAdmin() || event.isOwner(userId);

        if (!canEdit) {
            throw new AccessDeniedException("Você não tem permissão para remover este evento.");
        }

        eventRepository.delete(event);
    }
}
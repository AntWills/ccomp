package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.events.*;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.persistence.EventSpecification;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.news.util.SlugUtils;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.shared.dto.EventListItem;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.DebugUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public EventsServices(EventRepository eventRepository, UserManagement userManagement, EventMapper eventMapper, EditorServices editorServices) {
        this.eventRepository = eventRepository;
        this.userManagement = userManagement;
        this.eventMapper = eventMapper;
        this.editorServices = editorServices;
    }

    // ---- Consultas ----
    @Transactional(readOnly = true)
    public Optional<EventDTO> getById(Long eventId, UUID userId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    // O evento pode ser acessado se estiver publicado/unlisted OU se o usuário for dono/editor/admin
                    boolean allowed = event.isPubliclyAccessible()
                            || (userId != null && event.isOwner(userId))
                            || (userId != null && editorServices.isEditor(event, userId))
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
    public CursorPage<EventListItem> searchEventsWithFilters(
            EventsFilterRequest filter, String cursor, int pageSize) {
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

        Specification<Event> spec = EventSpecification.buildSpecByCursor(filter,
                CursorUtils.decode(cursor, EventCursor.class).orElse(null));

        int finalPageSize = pageSize;
        List<EventListItem> events = eventRepository.findBy(spec, query -> query
                .as(EventListItem.class)
                .sortBy(Sort.by(
                        Sort.Order.desc("startDate"),
                        Sort.Order.desc("id")
                ))
                .limit(finalPageSize + 1)
                .all());

        log.info("Quantidade de eventos retornados: {}", events.size());
        log.info("Horário da consulta: {}", LocalDateTime.now());

        boolean hasNext = events.size() > pageSize;
        List<EventListItem> page = hasNext ? events.subList(0, pageSize) : events;

        String nextCursor = hasNext
                ? CursorUtils.encode(new EventCursor(page.getLast().startDate(), page.getLast().id()))
                : null;

        return new CursorPage<>(page, nextCursor, null);
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

            if (eventRepository.findBySlug(slug).isEmpty()) {
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
                || editorServices.isEditor(event, userId);

        if (!canEdit)
            throw new AccessDeniedException("Você não tem permissão para alterar as configurações deste evento.");


        request.titleOpt().ifPresent(title -> {
            event.setTitle(title);
            event.setSlug(generateSlug(title));
        });

        eventMapper.updateEntityFromDto(request, event);

        eventRepository.save(event);

        return eventMapper.eventToEventDTO(event);
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
package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.events.core.dto.*;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.EventDslRepository;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.news.utils.SlugUtils;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventsServices {
    private final int MAX_PAGE_SIZE = 50;
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final UserManagement userManagement;
    private final EventMapper eventMapper;
    private final EventDslRepository eventDslRepository;
    private final EventAccessPolicy eventAccessPolicy;

    public EventsServices(
            EventRepository eventRepository, EventCache eventCache,
            UserManagement userManagement,
            EventMapper eventMapper,
            EventDslRepository eventDslRepository,
            EventAccessPolicy eventAccessPolicy) {
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.userManagement = userManagement;
        this.eventMapper = eventMapper;
        this.eventDslRepository = eventDslRepository;
        this.eventAccessPolicy = eventAccessPolicy;
    }

    // ---- Consultas ----
    @Transactional(readOnly = true)
    public Optional<EventDTO> getById(Long eventId, UUID userId) {
        return eventCache.findById(eventId)
                .map(event -> {
                    if (eventAccessPolicy.canView(event, userId)) return event;

                    throw new AccessDeniedException("Você não possui permissão para visualizar este evento.");
                });
    }

    @Transactional(readOnly = true)
    public Optional<EventDTO> getBySlug(String slug) {
        return eventCache.findBySlug(slug)
                .filter(EventDTO::isPublished);
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemDTO> searchEventsWithFilters(
            EventsFilterRequest filter, @Nullable String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

//        List<EventListItemView> events = eventBlaze.findByCursor(filter, decodedCursor, finalPageSize + 1);
        List<EventListItemDTO> events = eventDslRepository.findByCursor(filter, decodedCursor, finalPageSize + 1);

        return CursorUtils.buildPage(
                events,
                finalPageSize,
                e -> new EventCursor(e.startDate(), e.id()));
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemDTO> findAllByOwnerId(UUID ownerId, @Nullable String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

        List<EventListItemDTO> events = eventDslRepository.findAllByOwnerId(ownerId, decodedCursor, finalPageSize + 1);

        return CursorUtils.buildPage(events, finalPageSize,
                e -> new EventCursor(e.startDate(), e.id()));
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemDTO> findAllSubscriptions(UUID participantId, @Nullable String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

        List<EventListItemDTO> events = eventDslRepository
                .findAllSubscriptions(participantId, decodedCursor, finalPageSize + 1);


        return CursorUtils.buildPage(events, finalPageSize,
                e -> new EventCursor(e.startDate(), e.id()));
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemDTO> findMyEditableEvents(UUID editorId, @Nullable String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

        List<EventListItemDTO> events = eventDslRepository
                .findAllWhereUserIsEditor(editorId, decodedCursor, finalPageSize + 1);

        return CursorUtils.buildPage(events, finalPageSize,
                e -> new EventCursor(e.startDate(), e.id()));
    }

    // ---- Comandos ----
    @Transactional
    @CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
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


        dto.optionalStartDate().ifPresent(eventModel::setStartDate);
        dto.optionalEndDate().ifPresent(eventModel::setEndDate);

        var savedEvent = eventRepository.save(eventModel);
        eventCache.evict(savedEvent);

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
    @CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public EventDTO update(UpdateEventDTO request, Long eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event), userId))
            throw new AccessDeniedException("Você não tem permissão para alterar o status deste evento.");

        request.titleOpt().ifPresent(title -> {
            event.setTitle(title);
            event.setSlug(generateSlug(title));
        });

        eventMapper.updateEntityFromDto(request, event);

        var eventUpdated = eventRepository.save(event);
        eventCache.evict(eventUpdated);
        return eventMapper.eventToEventDTO(event);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public MessageResponse updateEventStatus(Long eventId, EnumEventStatus newStatus, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event), userId))
            throw new AccessDeniedException("Você não tem permissão para alterar o status deste evento.");

        // Executa a transição através dos métodos de domínio encapsulados
        switch (newStatus) {
            case PUBLISHED -> event.publish();
            case CANCELED -> event.cancel();
            case DRAFT -> event.moveToDraft();
            case UNLISTED -> event.unlist();
        }

        var eventUpdated = eventRepository.save(event);
        eventCache.evict(eventUpdated);

        return new MessageResponse("Status do evento alterado para: " + newStatus.name());
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public void delete(Long eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event), userId)) {
            throw new AccessDeniedException("Você não tem permissão para remover este evento.");
        }
        eventCache.evict(event);
        eventRepository.delete(event);
    }
}

package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.dto.EventListItem;
import com.ccomp.br.domain.events.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.dto.UpdateEventRequest;
import com.ccomp.br.domain.events.persistence.EventSpecification;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.news.util.SlugUtils;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
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

    // ---- Queries ----
    @Transactional(readOnly = true)
    public Optional<EventResponse> getById(Long eventId, UUID userId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    boolean allowed =
                            event.isOpen()
                                    || Optional.ofNullable(userId).filter(event::isOwner).isPresent();

                    if (allowed) return eventMapper.eventToEventResponse(event);

                    throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
                });
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItem> searchEventsWithFilters(
            EventsFilterRequest filter, LocalDateTime cursorStartDate, Long cursorId, int pageSize) {

        Specification<Event> spec = Specification.where(EventSpecification.isOpen())
                .and(EventSpecification.hasCategory(filter.eventCategory()))
                .and(EventSpecification.cursorBefore(cursorStartDate, cursorId));

        List<EventListItem> events = eventRepository.findBy(spec, query -> query
                .as(EventListItem.class)
                .sortBy(Sort.by(
                        Sort.Order.desc("startDate"),
                        Sort.Order.desc("id")   // tiebreaker precisa ter a MESMA direção do startDate
                ))
                .limit(pageSize + 1)
                .all());

        boolean hasNext = events.size() > pageSize;
        List<EventListItem> page = hasNext ? events.subList(0, pageSize) : events;

        EventListItem last = page.isEmpty() ? null : page.get(page.size() - 1);
        LocalDateTime nextCursorDate = hasNext ? last.startDate() : null;
        Long nextCursorId = hasNext ? last.id() : null;

        return new CursorPage<>(page, nextCursorDate, nextCursorId, hasNext);
    }

    // ---- Commands ----
    @Transactional
    public EventResponse create(UUID ownerId, CreateEventRequestDTO dto){
        if(userManagement.findById(ownerId).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + ownerId);

        var eventModel = Event.builder()
                .title(dto.title())
                .slug(generateSlug(dto.title()))
                .eventCategory(dto.eventCategory())
                .ownerId(ownerId).build();

        log.info("Salvando evento: {}", DebugUtils.printJson(eventModel));

        dto.optionalStartDate().ifPresent(eventModel::setStart);
        dto.optionalEndDate().ifPresent(eventModel::setEnd);

        var savedEvent = eventRepository.save(eventModel);

        return eventMapper.eventToEventResponse(savedEvent);
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

    public EventListItem update(UpdateEventRequest request, UUID userId) {
        Event event = eventRepository.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!event.isOwner(userId) && !editorServices.isEditor(event, userId))
            throw new AccessDeniedException("O usuario não tem permissão para alterar este recurso.");

        request.optionalTitle().ifPresent(title -> {
            event.setTitle(title);
            event.setSlug(generateSlug(title));
        });

       eventMapper.updateEntityFromDto(request, event);

       eventRepository.save(event);

       return eventMapper.eventToEventListItem(event);
    }
}

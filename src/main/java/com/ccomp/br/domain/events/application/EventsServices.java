package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.util.EventMapper;
import com.ccomp.br.domain.news.util.SlugUtils;
import com.ccomp.br.shared.dto.EventResponse;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.UserNotFaundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventsServices {
    private final EventRepository eventRepository;
    private final UserManagement userManagement;
    private final EventMapper eventMapper;


    public EventsServices(EventRepository eventRepository, UserManagement userManagement, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.userManagement = userManagement;
        this.eventMapper = eventMapper;
    }

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

    @Transactional
    public EventResponse create(UUID ownerId, CreateEventRequestDTO dto){
        if(userManagement.findById(ownerId).isEmpty()) throw new UserNotFaundException("Owner not found with ID: " + ownerId);

        var eventModel = Event.builder()
                .title(dto.title())
                .slug(generateSlug(dto.title()))
                .ownerId(ownerId).build();

        dto.optionalStartDate().ifPresent(eventModel::setStart);
        dto.optionalEndDate().ifPresent(eventModel::setEnd);

        var savedEvent = eventRepository.save(new Event(dto.title(), ownerId));

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
}

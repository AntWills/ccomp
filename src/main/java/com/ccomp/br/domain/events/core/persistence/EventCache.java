package com.ccomp.br.domain.events.core.persistence;

import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class EventCache {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final Clock clock;

    public EventCache(EventRepository eventRepository, EventMapper eventMapper, Clock clock) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.EVENTS_BY_ID, key = "#eventId")
    public Optional<EventDTO> findById(Long eventId) {
        return eventRepository
                .findById(eventId).map(event -> eventMapper.eventToEventDTO(event, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.EVENTS_BY_SLUG, key = "#slug")
    public Optional<EventDTO> findBySlug(String slug) {
        return eventRepository
                .findBySlug(slug).map(event -> eventMapper.eventToEventDTO(event, LocalDateTime.now(clock)));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.EVENTS_BY_ID, key = "#event.id"),
            @CacheEvict(cacheNames = CacheNames.EVENTS_BY_SLUG, key = "#event.slug")
    })
    public void evict(Event event) {}
}

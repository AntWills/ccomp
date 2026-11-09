package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.shared.cache.CacheNames;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class EventGuestCache {
    private final EventGuestRepository eventGuestRepository;
    private final EventGuestDslRepository eventGuestDslRepository;

    public EventGuestCache(
            EventGuestRepository eventGuestRepository,
            EventGuestDslRepository eventGuestDslRepository) {
        this.eventGuestRepository = eventGuestRepository;
        this.eventGuestDslRepository = eventGuestDslRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EVENT_GUESTS,
            key = "#eventId + ':' + #isManager + ':' + " +
                    "(#cursor == null || #cursor.id() == null || #cursor.createdAt() == null " +
                    "? 'first' : #cursor.createdAt() + '-' + #cursor.id()) + ':' + #limit")
    public List<EventGuestDTO> findGuestsWithCursor(
            Long eventId, boolean isManager, @Nullable GuestCursor cursor, int limit) {
        return eventGuestDslRepository.findGuestsWithCursor(eventId, isManager, cursor, limit);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EVENT_GUEST_BY_ID,
            key = "#eventId + ':' + #guestId",
            unless = "#result == null")
    public Optional<EventGuestDTO> findByIdAndEventId(Long eventId, Long guestId) {
        return eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .map(this::toDto);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.EVENT_GUEST_BY_ID,
                    key = "#guest.event.id + ':' + #guest.id"),
            @CacheEvict(cacheNames = CacheNames.EVENT_GUESTS, allEntries = true)
    })
    public void evict(EventGuest guest) {}

    private EventGuestDTO toDto(EventGuest guest) {
        return new EventGuestDTO(
                guest.getId(),
                guest.getEventId(),
                guest.getName(),
                guest.getDescription(),
                guest.getImageKey(),
                guest.getStatus(),
                guest.getVisibility(),
                guest.getCreatedAt());
    }
}

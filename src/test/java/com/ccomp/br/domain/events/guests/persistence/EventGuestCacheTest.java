package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventGuestCacheTest {
    private static final Long EVENT_ID = 12L;
    private static final Long GUEST_ID = 35L;

    @Mock
    private EventGuestRepository eventGuestRepository;

    @Mock
    private EventGuestDslRepository eventGuestDslRepository;

    private EventGuestCache eventGuestCache;

    @BeforeEach
    void setUp() {
        eventGuestCache = new EventGuestCache(eventGuestRepository, eventGuestDslRepository);
    }

    @Test
    void findGuestsWithCursorDelegatesWithEventAndVisibilityArguments() {
        GuestCursor cursor = new GuestCursor(20L, LocalDateTime.parse("2026-11-08T12:00:00"));
        EventGuestDTO guest = new EventGuestDTO(GUEST_ID, EVENT_ID, "Ada", null, null,
                EnumGuestStatus.CONFIRMED, EnumGuestVisibility.PUBLIC, cursor.createdAt());
        when(eventGuestDslRepository.findGuestsWithCursor(EVENT_ID, false, cursor, 20))
                .thenReturn(List.of(guest));

        assertThat(eventGuestCache.findGuestsWithCursor(EVENT_ID, false, cursor, 20))
                .containsExactly(guest);

        verify(eventGuestDslRepository).findGuestsWithCursor(EVENT_ID, false, cursor, 20);
    }

    @Test
    void findByIdAndEventIdMapsEntityToCacheableDto() {
        Event event = Event.builder().id(EVENT_ID).build();
        EventGuest guest = EventGuest.builder()
                .id(GUEST_ID)
                .event(event)
                .eventId(EVENT_ID)
                .name("Ada Lovelace")
                .description("Palestrante")
                .imageKey("events/12/guests/35/ada.webp")
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(LocalDateTime.parse("2026-11-08T12:00:00"))
                .build();
        when(eventGuestRepository.findByIdAndEventId(GUEST_ID, EVENT_ID)).thenReturn(Optional.of(guest));

        Optional<EventGuestDTO> result = eventGuestCache.findByIdAndEventId(EVENT_ID, GUEST_ID);

        assertThat(result).contains(new EventGuestDTO(
                GUEST_ID,
                EVENT_ID,
                "Ada Lovelace",
                "Palestrante",
                "events/12/guests/35/ada.webp",
                EnumGuestStatus.CONFIRMED,
                EnumGuestVisibility.PUBLIC,
                LocalDateTime.parse("2026-11-08T12:00:00")));
        verify(eventGuestRepository).findByIdAndEventId(GUEST_ID, EVENT_ID);
    }
}

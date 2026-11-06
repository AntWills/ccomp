package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterial;
import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.application.EventStorageFileKeyCollector;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.materials.persistence.EventMaterial;
import com.ccomp.br.domain.events.materials.persistence.EventMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStorageFileKeyCollectorTest {
    @Mock private EventMaterialRepository eventMaterialRepository;
    @Mock private EventActivityRepository activityRepository;
    @Mock private ActivityMaterialRepository activityMaterialRepository;
    @Mock private EventGuestRepository eventGuestRepository;
    @Mock private EventCoverTemplateRepository coverTemplateRepository;

    @Test
    void collectsAllKeysBeforeTheirOwningRowsAreDeletedAndSkipsNullsAndDuplicates() {
        Event event = mock(Event.class);
        EventActivity activity = mock(EventActivity.class);
        EventGuest guestWithImage = mock(EventGuest.class);
        EventGuest guestWithoutImage = mock(EventGuest.class);
        when(event.getId()).thenReturn(7L);
        when(event.getCoverImageKey()).thenReturn("events/7/cover.png");
        when(activity.getId()).thenReturn(22L);
        when(guestWithImage.getImageKey()).thenReturn("events/7/guest.png");
        when(guestWithoutImage.getImageKey()).thenReturn(null);
        when(eventMaterialRepository.findAllByEventId(7L)).thenReturn(List.of(
                EventMaterial.builder().fileKey("events/7/material.pdf").build(),
                EventMaterial.builder().fileKey(null).build(),
                EventMaterial.builder().fileKey("events/7/cover.png").build()));
        when(activityRepository.findAllByEventId(7L)).thenReturn(List.of(activity));
        when(activityMaterialRepository.findAllByActivityIdIn(List.of(22L))).thenReturn(List.of(
                ActivityMaterial.builder().fileKey("activities/22/material.pdf").build()));
        when(eventGuestRepository.findAllByEventId(7L)).thenReturn(List.of(guestWithImage, guestWithoutImage));
        when(coverTemplateRepository.findByEventId(7L)).thenReturn(Optional.of(
                EventCoverTemplate.builder().coverTemplateKey("events/7/certificate-cover.png").build()));

        EventStorageFileKeyCollector collector = new EventStorageFileKeyCollector(
                eventMaterialRepository,
                activityRepository,
                activityMaterialRepository,
                eventGuestRepository,
                coverTemplateRepository);

        assertThat(collector.collect(event)).containsExactly(
                "events/7/cover.png",
                "events/7/material.pdf",
                "activities/22/material.pdf",
                "events/7/guest.png",
                "events/7/certificate-cover.png");
    }

    @Test
    void skipsActivityMaterialLookupWhenEventHasNoActivities() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(9L);
        when(eventMaterialRepository.findAllByEventId(9L)).thenReturn(List.of());
        when(activityRepository.findAllByEventId(9L)).thenReturn(List.of());
        when(eventGuestRepository.findAllByEventId(9L)).thenReturn(List.of());
        when(coverTemplateRepository.findByEventId(9L)).thenReturn(Optional.empty());

        EventStorageFileKeyCollector collector = new EventStorageFileKeyCollector(
                eventMaterialRepository,
                activityRepository,
                activityMaterialRepository,
                eventGuestRepository,
                coverTemplateRepository);

        assertThat(collector.collect(event)).isEmpty();
    }
}

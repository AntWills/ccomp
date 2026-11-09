package com.ccomp.br.domain.events.activities.application;

import java.util.Optional;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.instructor.application.InstructorService;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityUpdateAuthorizationTest {
    @Mock private EventRepository eventRepository;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventCache eventCache;
    @Mock private EventMapper eventMapper;
    @Mock private EventActivityRepository activityRepository;
    @Mock private EventActivityDslRepository activityDslRepository;
    @Mock private ActivityMapper activityMapper;
    @Mock private ActivityCache activityCache;
    @Mock private GuestService guestService;
    @Mock private InstructorService instructorService;
    @InjectMocks private ActivityServices service;

    @Test
    void confirmedActiveInstructorUsesSharedActivityUpdateFlow() {
        long activityId = 31L;
        UUID userId = UUID.randomUUID();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        ActivityDTO expected = mock(ActivityDTO.class);
        UpdateActivityDTO request = UpdateActivityDTO.builder().title("Atualizado").build();
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(4L);
        when(eventCache.findById(4L)).thenReturn(Optional.of(event));
        when(instructorService.canEditActivity(activityId, userId)).thenReturn(true);
        when(activityRepository.save(activity)).thenReturn(activity);
        when(activityMapper.eventActivityToActivityDTO(activity)).thenReturn(expected);

        assertThat(service.updateActivity(userId, activityId, request)).isSameAs(expected);

        verify(activityMapper).updateEventActivityFromRequest(request, activity);
        verify(activityRepository).save(activity);
        verify(activityCache).evict(activity);
        verifyNoInteractions(guestService);
    }

    @Test
    void linkedActiveGuestUsesSharedActivityUpdateFlow() {
        long activityId = 31L;
        UUID userId = UUID.randomUUID();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(4L);
        when(eventCache.findById(4L)).thenReturn(Optional.of(event));
        when(guestService.canEditActivity(activityId, userId)).thenReturn(true);
        when(activityRepository.save(activity)).thenReturn(activity);

        service.updateActivity(userId, activityId, UpdateActivityDTO.builder().title("Atualizado").build());

        verify(activityRepository).save(activity);
        verify(instructorService).canEditActivity(activityId, userId);
    }

    @Test
    void rejectsInactiveOrUnlinkedNonManagerFromSharedActivityUpdate() {
        long activityId = 31L;
        UUID userId = UUID.randomUUID();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(4L);
        when(eventCache.findById(4L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.updateActivity(userId, activityId,
                UpdateActivityDTO.builder().title("Atualizado").build()))
                .isInstanceOf(AccessDeniedException.class);

        verify(activityRepository, never()).save(any());
    }
}

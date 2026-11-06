package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.EventGuestDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestSelfPermissionsTest {
    @Mock private EventGuestRepository eventGuestRepository;
    @Mock private EventCache eventCache;
    @Mock private ActivityCache activityCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventGuestDslRepository eventGuestDslRepository;
    @Mock private ActivityGuestDslRepository activityGuestDslRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventActivityRepository activityRepository;
    @Mock private ActivityGuestRepository activityGuestRepository;
    @Mock private UserManagement userManagement;
    @Mock private ActivityMapper activityMapper;
    @InjectMocks private GuestService service;

    @Test
    void linkedGuestCanEditOwnProfileButIsNotTreatedAsEventManager() {
        long eventId = 8L;
        long guestId = 3L;
        UUID userId = UUID.randomUUID();
        EventGuest guest = mock(EventGuest.class);
        EventDTO event = mock(EventDTO.class);
        when(eventGuestRepository.findByIdAndEventId(guestId, eventId)).thenReturn(Optional.of(guest));
        when(guest.getUserId()).thenReturn(userId);
        when(eventCache.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canEdit(event, userId)).thenReturn(false);

        service.updateGuestProfile(eventId, guestId, userId,
                new UpdateGuestProfileDTO("Nome atualizado", "Descrição atualizada"));

        verify(guest).setName("Nome atualizado");
        verify(guest).setDescription("Descrição atualizada");
        verify(eventGuestRepository).save(guest);
    }

    @Test
    void linkedGuestCanEditOnlyAnActivityWhereTheirProfileIsAssociated() {
        long activityId = 13L;
        UUID userId = UUID.randomUUID();
        EventActivity activity = mock(EventActivity.class);
        ActivityDTO expected = mock(ActivityDTO.class);
        UpdateActivityDTO request = UpdateActivityDTO.builder().title("Título novo").build();
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, userId)).thenReturn(true);
        when(activityRepository.save(activity)).thenReturn(activity);
        when(activityMapper.eventActivityToActivityDTO(activity)).thenReturn(expected);

        assertThat(service.updateActivityAsGuest(userId, activityId, request)).isSameAs(expected);

        verify(activityMapper).updateEventActivityFromRequest(request, activity);
        verify(activityCache).evict(activity);
    }

    @Test
    void linkedGuestCannotEditAnUnassociatedActivity() {
        long activityId = 13L;
        UUID userId = UUID.randomUUID();
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(mock(EventActivity.class)));
        when(activityGuestRepository.existsByActivityIdAndEventGuestUserId(activityId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateActivityAsGuest(userId, activityId,
                UpdateActivityDTO.builder().title("Título novo").build()))
                .isInstanceOf(AccessDeniedException.class);
        verify(activityRepository, never()).save(any());
    }
}

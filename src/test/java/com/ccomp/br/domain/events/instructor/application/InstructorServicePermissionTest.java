package com.ccomp.br.domain.events.instructor.application;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.instructor.dto.ActivityParticipantDTO;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InstructorServicePermissionTest {
    @Mock private EventActivityRepository activityRepository;
    @Mock private EventInstructorRepository instructorRepository;
    @Mock private InstructorDslRepository instructorDslRepository;
    @Mock private ActivityGuestDslRepository activityGuestDslRepository;
    @Mock private EventCache eventCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private UserManagement userManagement;
    @InjectMocks private InstructorService service;

    @Test
    void activityParticipantsAreCombinedAndMarkedAsGuestOrInstructor() {
        long activityId = 22L;
        long eventId = 6L;
        UUID managerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        EventInstructor instructor = EventInstructor.builder()
                .id(7L).activityId(activityId).name("Auxiliar")
                .role(EnumInstructorRole.ASSISTANT).createdAt(now).build();
        ActivityGuestDTO guest = new ActivityGuestDTO(
                9L, activityId, 3L, "Palestrante destaque", "Perfil", null,
                EnumActivityGuestRole.SPECIAL_GUEST, EnumGuestStatus.CONFIRMED,
                EnumGuestVisibility.PUBLIC, true, now.minusMinutes(1));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(eventId);
        when(eventCache.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canView(event, managerId)).thenReturn(true);
        when(eventAccessPolicy.canEdit(event, managerId)).thenReturn(false);
        when(instructorDslRepository.findByActivity(activityId)).thenReturn(List.of(instructor));
        when(activityGuestDslRepository.findActivityGuestsForParticipants(activityId, false)).thenReturn(List.of(guest));

        List<ActivityParticipantDTO> participants = service.searchByActivity(activityId, managerId);

        assertThat(participants).hasSize(2);
        assertThat(participants.get(0).isGuest()).isFalse();
        assertThat(participants.get(0).role()).isEqualTo("ASSISTANT");
        assertThat(participants.get(1).isGuest()).isTrue();
        assertThat(participants.get(1).role()).isEqualTo("SPECIAL_GUEST");
    }

    @Test
    void confirmedInstructorWithActiveAccountCanEditActivity() {
        long activityId = 22L;
        UUID userId = UUID.randomUUID();
        EventInstructor instructor = EventInstructor.builder()
                .activityId(activityId)
                .userId(userId)
                .name("Ministrante")
                .build();
        when(userManagement.isAccountActive(userId)).thenReturn(true);
        when(instructorRepository.findByActivityIdAndUserId(activityId, userId))
                .thenReturn(Optional.of(instructor));

        assertThat(service.canEditActivity(activityId, userId)).isTrue();
    }

    @Test
    void inactiveAccountCannotEditEvenWithConfirmedInstructorLink() {
        long activityId = 22L;
        UUID userId = UUID.randomUUID();
        when(userManagement.isAccountActive(userId)).thenReturn(false);

        assertThat(service.canEditActivity(activityId, userId)).isFalse();
    }

    @Test
    void canceledInstructorCannotEditActivity() {
        long activityId = 22L;
        UUID userId = UUID.randomUUID();
        EventInstructor instructor = EventInstructor.builder()
                .activityId(activityId)
                .userId(userId)
                .name("Ministrante")
                .status(EnumInstructorStatus.CANCELED)
                .build();
        when(userManagement.isAccountActive(userId)).thenReturn(true);
        when(instructorRepository.findByActivityIdAndUserId(activityId, userId))
                .thenReturn(Optional.of(instructor));

        assertThat(service.canEditActivity(activityId, userId)).isFalse();
    }

    @Test
    void managerCanDeleteInstructorWithoutDeletingActivity() {
        long activityId = 22L;
        long instructorId = 7L;
        long eventId = 6L;
        UUID managerId = UUID.randomUUID();
        EventInstructor instructor = EventInstructor.builder().id(instructorId).activityId(activityId).build();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        when(instructorRepository.findByIdAndActivityId(instructorId, activityId)).thenReturn(Optional.of(instructor));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(eventId);
        when(eventCache.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAccessPolicy.canEdit(event, managerId)).thenReturn(true);

        service.deleteInstructor(activityId, instructorId, managerId);

        verify(instructorRepository).delete(instructor);
    }
}

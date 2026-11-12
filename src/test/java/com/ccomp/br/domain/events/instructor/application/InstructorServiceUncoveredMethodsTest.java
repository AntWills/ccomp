package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfile;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfileRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorServiceUncoveredMethodsTest {
    @Mock private EventActivityRepository activities;
    @Mock private EventInstructorRepository instructors;
    @Mock private InstructorDslRepository instructorDsl;
    @Mock private ActivityGuestDslRepository guests;
    @Mock private EventCache events;
    @Mock private EventAccessPolicy policy;
    @Mock private UserManagement users;
    @Mock private EventInstructorProfileRepository profiles;

    private InstructorService service() {
        return new InstructorService(activities, instructors, instructorDsl, guests, events, policy, users, profiles);
    }

    @Test
    void addProfileToActivityCreatesAssociation() {
        long eventId = 2L;
        long profileId = 3L;
        long activityId = 4L;
        UUID requester = UUID.randomUUID();
        EventActivity activity = mock(EventActivity.class);
        EventDTO event = mock(EventDTO.class);
        EventInstructorProfile profile = EventInstructorProfile.builder().id(profileId).name("T").build();

        when(activities.findById(activityId)).thenReturn(Optional.of(activity));
        when(activity.getEventId()).thenReturn(eventId);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(policy.canEdit(event, requester)).thenReturn(true);
        when(profiles.findByIdAndEventId(profileId, eventId)).thenReturn(Optional.of(profile));
        when(instructors.findByActivityIdAndProfile_Id(activityId, profileId)).thenReturn(Optional.empty());

        service().addProfileToActivity(eventId, profileId, activityId, requester, null);

        verify(instructors).save(any(EventInstructor.class));
    }

    @Test
    void addProfileToActivityFailsForMissingActivity() {
        when(activities.findById(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().addProfileToActivity(2L,3L,4L,UUID.randomUUID(),null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchByEventReturnsPage() {
        UUID requester = UUID.randomUUID();
        EventDTO event = mock(EventDTO.class);
        when(events.findById(2L)).thenReturn(Optional.of(event));
        when(policy.canView(event, requester)).thenReturn(true);
        when(instructorDsl.findByEvent(eq(2L), any(), eq(3))).thenReturn(List.of());

        assertThat(service().searchByEvent(2L,requester,null,2).content()).isEmpty();
    }

    @Test
    void searchByEventRejectsUnauthorizedRequester() {
        UUID requester = UUID.randomUUID();
        EventDTO event = mock(EventDTO.class);
        when(events.findById(2L)).thenReturn(Optional.of(event));
        when(policy.canView(event, requester)).thenReturn(false);

        assertThatThrownBy(() -> service().searchByEvent(2L,requester,null,2)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelMyLinkCancelsConfirmedLink() {
        UUID instructorId = UUID.randomUUID();
        EventInstructor instructor = EventInstructor.builder().activityId(4L).userId(instructorId).build();
        when(instructors.findByActivityIdAndUserId(4L, instructorId)).thenReturn(Optional.of(instructor));

        assertThat(service().cancelMyLink(instructorId,4L).response()).contains("cancelado");
        verify(instructors).save(instructor);
    }

    @Test
    void cancelMyLinkFailsWhenNoLinkExists() {
        UUID instructorId = UUID.randomUUID();
        when(instructors.findByActivityIdAndUserId(4L, instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cancelMyLink(instructorId,4L)).isInstanceOf(AccessDeniedException.class);
    }
}

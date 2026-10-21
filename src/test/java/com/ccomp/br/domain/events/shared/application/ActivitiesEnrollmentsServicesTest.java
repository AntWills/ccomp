package com.ccomp.br.domain.events.shared.application;

import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitiesEnrollmentsServicesTest {
    @Mock private EventActivityRepository activityRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EnrollmentActivityRepository enrollmentActivityRepository;
    @Mock private EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    @Mock private EventActivityDslRepository eventActivityDslRepository;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventMapper eventMapper;
    @InjectMocks private ActivitiesEnrollmentsServices service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void inscribePersistsEnrollmentForEventRegistrant() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment enrollment = mock(Enrollment.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activity.isPublic()).thenReturn(false);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L)).thenReturn(Optional.empty());
        when(activity.getEvent()).thenReturn(event);
        when(event.isSchedulerPrevent()).thenReturn(false);
        when(enrollmentRepository.findByUserIdAndEvent(userId, event)).thenReturn(Optional.of(enrollment));

        var response = service.inscribe(userId, 9L);

        assertThat(response.response()).isEqualTo("Inscrição na atividade realizada com sucesso.");
        verify(enrollmentActivityRepository).save(any(EnrollmentActivity.class));
    }

    @Test
    void inscribeRejectsMissingActivity() {
        when(activityRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inscribe(userId, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(enrollmentActivityRepository);
    }

    @Test
    void listSubscriptionsUsesCursorAndReturnsPage() {
        when(eventActivityDslRepository.findSubscriptionsWithCursor(userId, 4L, null, 11))
                .thenReturn(List.of());

        var page = service.listSubscriptionsByCursor(userId, 4L, null, 10);

        assertThat(page.content()).isEmpty();
        verify(eventActivityDslRepository).findSubscriptionsWithCursor(userId, 4L, null, 11);
    }

    @Test
    void listConflictingActivitiesUsesActivityAndCursorQuery() {
        EventActivity activity = mock(EventActivity.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(eventActivityDslRepository.findConflictingActivities(userId, activity, null, 11))
                .thenReturn(List.of());

        var page = service.listConflictingActivities(userId, 9L, null, 10);

        assertThat(page.content()).isEmpty();
        verify(eventActivityDslRepository).findConflictingActivities(userId, activity, null, 11);
    }

    @Test
    void unsubscribeDeletesExistingEnrollment() {
        EnrollmentActivity enrollment = mock(EnrollmentActivity.class);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L))
                .thenReturn(Optional.of(enrollment));

        var response = service.unsubscribe(userId, 9L);

        assertThat(response.response()).isEqualTo("Inscrição removida com sucesso.");
        verify(enrollmentActivityRepository).delete(enrollment);
    }

    @Test
    void findAllUsersFromActivityChecksEditPermissionBeforeQuerying() {
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        when(eventActivityDslRepository.findEventByActivityId(9L)).thenReturn(Optional.of(event));
        when(eventMapper.eventToEventDTO(event)).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
        when(enrollmentActivityDslRepository.findAllUsersByActivityId(9L, null, 11)).thenReturn(List.of());

        var page = service.findAllUsersFromActivity(userId, 9L, null, 10);

        assertThat(page.content()).isEmpty();
        verify(enrollmentActivityDslRepository).findAllUsersByActivityId(9L, null, 11);
    }

    @Test
    void findAllUsersFromActivityRejectsUserWithoutEditPermission() {
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        when(eventActivityDslRepository.findEventByActivityId(9L)).thenReturn(Optional.of(event));
        when(eventMapper.eventToEventDTO(event)).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.findAllUsersFromActivity(userId, 9L, null, 10))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(enrollmentActivityDslRepository);
    }
}

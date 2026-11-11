package com.ccomp.br.domain.events.shared.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentActivityCursor;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.dto.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.SchedulingConflictException;
import com.ccomp.br.shared.utils.CursorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitiesEnrollmentsServicesTest {
    @Mock private EventActivityRepository activityRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EnrollmentActivityRepository enrollmentActivityRepository;
    @Mock private EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    @Mock private EventActivityDslRepository eventActivityDslRepository;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private EventMapper eventMapper;
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-11-08T12:10:00Z"), ZoneId.of("America/Sao_Paulo"));
    @InjectMocks private ActivitiesEnrollmentsServices service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void inscribePersistsEnrollmentForEventRegistrant() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment enrollment = mock(Enrollment.class);
        stubActivityEnrollment(activity, event, enrollment);
        when(activity.isPublic()).thenReturn(false);
        when(activity.getMaxCapacity()).thenReturn(null);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L)).thenReturn(Optional.empty());
        when(event.isSchedulerPrevent()).thenReturn(false);

        var response = service.inscribe(userId, 9L);

        assertThat(response.response()).isEqualTo("Inscrição na atividade realizada com sucesso.");
        verify(enrollmentActivityRepository).save(any(EnrollmentActivity.class));
        verify(activity).registerActiveEnrollment();
    }

    @Test
    void managerCanEnrollActiveEventParticipantInActivity() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment enrollment = mock(Enrollment.class);
        EventDTO eventDTO = mock(EventDTO.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        when(activity.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(4L);
        when(event.isSchedulerPrevent()).thenReturn(false);
        when(eventMapper.eventToEventDTO(eq(event), any())).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
        when(enrollmentRepository.findByIdAndEventId(12L, 4L)).thenReturn(Optional.of(enrollment));
        when(enrollment.isActive()).thenReturn(true);
        when(enrollmentActivityRepository.findByEnrollmentIdAndActivityId(12L, 9L)).thenReturn(Optional.empty());
        when(activity.getMaxCapacity()).thenReturn(10);
        when(activity.getActiveEnrollmentCount()).thenReturn(2);

        var response = service.enrollParticipant(userId, 9L, 12L);

        assertThat(response.response()).isEqualTo("Pessoa inscrita na atividade com sucesso.");
        verify(enrollmentActivityRepository).save(any(EnrollmentActivity.class));
        verify(activity).registerActiveEnrollment();
    }

    @Test
    void managerEnrollmentRejectsUserWithoutEditPermission() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activity.getEvent()).thenReturn(event);
        when(eventMapper.eventToEventDTO(eq(event), any())).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.enrollParticipant(userId, 9L, 12L))
                .isInstanceOf(AccessDeniedException.class);
        verify(enrollmentActivityRepository, never()).save(any());
    }

    @Test
    void managerEnrollmentRejectsInactiveEventEnrollment() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        Enrollment enrollment = mock(Enrollment.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        when(activity.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(4L);
        when(eventMapper.eventToEventDTO(eq(event), any())).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
        when(enrollmentRepository.findByIdAndEventId(12L, 4L)).thenReturn(Optional.of(enrollment));
        when(enrollment.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.enrollParticipant(userId, 9L, 12L))
                .isInstanceOf(ConflictException.class);
        verify(enrollmentActivityRepository, never()).save(any());
    }

    @Test
    void inscribeAllowsEventRegistrantWithNoExistingActivityEnrollment() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment enrollment = mock(Enrollment.class);
        stubActivityEnrollment(activity, event, enrollment);
        when(activity.isPublic()).thenReturn(false);
        when(activity.getMaxCapacity()).thenReturn(null);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L)).thenReturn(Optional.empty());
        when(event.isSchedulerPrevent()).thenReturn(true);
        when(eventActivityDslRepository.findAllConflictingActivities(userId, activity)).thenReturn(List.of());

        var response = service.inscribe(userId, 9L);

        assertThat(response.response()).isEqualTo("Inscrição na atividade realizada com sucesso.");
        verify(eventActivityDslRepository).findAllConflictingActivities(userId, activity);
        verify(enrollmentActivityRepository).save(any(EnrollmentActivity.class));
    }

    @Test
    void inscribeReturnsConflictingActivitiesWhenScheduleOverlaps() {
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment enrollment = mock(Enrollment.class);
        ActivityDTO conflictingActivity = new ActivityDTO(7L, 4L, "Atividade já inscrita", null,
                0L, "Sala 1", null, null, null, null, null);
        stubActivityEnrollment(activity, event, enrollment);
        when(activity.isPublic()).thenReturn(false);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L)).thenReturn(Optional.empty());
        when(event.isSchedulerPrevent()).thenReturn(true);
        when(eventActivityDslRepository.findAllConflictingActivities(userId, activity))
                .thenReturn(List.of(conflictingActivity));

        assertThatThrownBy(() -> service.inscribe(userId, 9L))
                .isInstanceOfSatisfying(SchedulingConflictException.class, exception ->
                        assertThat(exception.toErrorResponse("/api/events/activities/9/subscribe")
                                .getConflicts()).satisfies(conflicts -> {
                                    assertThat(conflicts).hasSize(1);
                                    assertThat(conflicts.getFirst()).isEqualTo(conflictingActivity);
                                }));
        verify(enrollmentActivityRepository, never()).save(any(EnrollmentActivity.class));
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
    void unsubscribeRunsInsideTransaction() throws NoSuchMethodException {
        var transaction = ActivitiesEnrollmentsServices.class
                .getMethod("unsubscribe", UUID.class, Long.class)
                .getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
    }

    @Test
    void unsubscribeDeletesExistingEnrollmentAfterLockingActiveEventEnrollment() {
        EnrollmentActivity enrollment = mock(EnrollmentActivity.class);
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        when(activity.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(4L);
        when(enrollment.getEnrollmentId()).thenReturn(12L);
        Enrollment lockedEnrollment = mock(Enrollment.class);
        when(lockedEnrollment.isActive()).thenReturn(true);
        when(enrollmentRepository.findByIdAndEventId(12L, 4L)).thenReturn(Optional.of(lockedEnrollment));
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L))
                .thenReturn(Optional.of(enrollment));

        var response = service.unsubscribe(userId, 9L);

        assertThat(response.response()).isEqualTo("Inscrição removida com sucesso.");
        verify(enrollmentRepository).findByIdAndEventId(12L, 4L);
        verify(activityRepository).findByIdForUpdate(9L);
        verify(enrollmentActivityRepository).delete(enrollment);
        verify(activity).releaseActiveEnrollment();
    }

    @Test
    void unsubscribeRejectsInactiveEventEnrollmentWithoutDeletingActivityEnrollment() {
        EnrollmentActivity enrollment = mock(EnrollmentActivity.class);
        EventActivity activity = mock(EventActivity.class);
        Event event = mock(Event.class);
        Enrollment lockedEnrollment = mock(Enrollment.class);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, 9L))
                .thenReturn(Optional.of(enrollment));
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activity.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(4L);
        when(enrollment.getEnrollmentId()).thenReturn(12L);
        when(enrollmentRepository.findByIdAndEventId(12L, 4L)).thenReturn(Optional.of(lockedEnrollment));
        when(lockedEnrollment.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.unsubscribe(userId, 9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(activityRepository, never()).findByIdForUpdate(9L);
        verifyNoInteractions(enrollmentActivityRepository);
        verify(activity, never()).releaseActiveEnrollment();
    }

    private void stubActivityEnrollment(EventActivity activity, Event event, Enrollment enrollment) {
        when(activityRepository.findById(9L)).thenReturn(Optional.of(activity));
        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        when(activity.isPublic()).thenReturn(false);
        when(activity.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(4L);
        when(enrollmentRepository.findForActivityRegistrationByUserIdAndEventId(userId, 4L))
                .thenReturn(Optional.of(enrollment));
        when(enrollment.isActive()).thenReturn(true);
    }

    @Test
    void findAllUsersFromActivityChecksEditPermissionBeforeQuerying() {
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        when(eventActivityDslRepository.findEventByActivityId(9L)).thenReturn(Optional.of(event));
        when(eventMapper.eventToEventDTO(any(), any())).thenReturn(eventDTO);
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
        when(eventMapper.eventToEventDTO(any(), any())).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.findAllUsersFromActivity(userId, 9L, null, 10))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(enrollmentActivityDslRepository);
    }

    @Test
    void findAllUsersFromActivityEmitsCursorCompatibleWithItsDecoder() {
        Event event = mock(Event.class);
        EventDTO eventDTO = mock(EventDTO.class);
        var firstDate = java.time.LocalDateTime.parse("2026-11-08T10:00:00");
        var secondDate = firstDate.minusMinutes(10);
        when(eventActivityDslRepository.findEventByActivityId(9L)).thenReturn(Optional.of(event));
        when(eventMapper.eventToEventDTO(any(), any())).thenReturn(eventDTO);
        when(eventAccessPolicy.canEdit(eventDTO, userId)).thenReturn(true);
        when(enrollmentActivityDslRepository.findAllUsersByActivityId(9L, null, 2)).thenReturn(List.of(
                new UserActivitySummaryDTO(null, false, 52L, firstDate),
                new UserActivitySummaryDTO(null, false, 51L, secondDate)));

        var page = service.findAllUsersFromActivity(userId, 9L, null, 1);

        assertThat(CursorUtils.decode(page.nextCursor(), EnrollmentActivityCursor.class))
                .isEqualTo(new EnrollmentActivityCursor(52L, firstDate));
        String legacyCursor = CursorUtils.encode(new EnrollmentsCursor(null, firstDate, 52L));
        assertThat(CursorUtils.decode(legacyCursor, EnrollmentActivityCursor.class))
                .isEqualTo(new EnrollmentActivityCursor(52L, firstDate));
    }
}

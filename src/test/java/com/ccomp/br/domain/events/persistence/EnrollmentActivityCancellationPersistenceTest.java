package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.shared.utils.TextSearchSession;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.dto.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, EnrollmentActivityDslRepository.class, EventActivityDslRepository.class,
        TextSearchSession.class})
class EnrollmentActivityCancellationPersistenceTest {
    @Autowired private EventRepository eventRepository;
    @Autowired private EventActivityRepository activityRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private EnrollmentActivityRepository enrollmentActivityRepository;
    @Autowired private EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    @Autowired private EventActivityDslRepository eventActivityDslRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void managerCancellationPreservesDatabaseLinksAttendanceAndEventCertificateCheckIn() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LocalDateTime attendedAt = LocalDateTime.parse("2026-11-08T12:00:00");
        Fixture fixture = persistFixture(userId, EnumEnrollmentState.CHECKED_IN, true, attendedAt);

        EnrollmentActivity enrollmentActivity = enrollmentActivityRepository
                .findByEnrollmentIdAndActivityId(fixture.enrollment().getId(), fixture.activity().getId())
                .orElseThrow();
        EventActivity activity = activityRepository.findById(fixture.activity().getId()).orElseThrow();
        enrollmentActivity.cancel(managerId);
        enrollmentActivityRepository.save(enrollmentActivity);
        activity.releaseActiveEnrollment();
        activityRepository.saveAndFlush(activity);
        enrollmentActivityRepository.flush();
        entityManager.clear();

        EnrollmentActivity persisted = enrollmentActivityRepository
                .findByEnrollmentIdAndActivityId(fixture.enrollment().getId(), fixture.activity().getId())
                .orElseThrow();
        EventActivity persistedActivity = activityRepository.findById(fixture.activity().getId()).orElseThrow();
        Enrollment persistedEventEnrollment = enrollmentRepository.findById(fixture.enrollment().getId()).orElseThrow();

        assertThat(persisted.isCanceled()).isTrue();
        assertThat(persisted.getCanceledBy()).isEqualTo(managerId);
        assertThat(persisted.getAttendedAt()).isEqualTo(attendedAt);
        assertThat(persisted.getEnrollment().getId()).isEqualTo(fixture.enrollment().getId());
        assertThat(persisted.getActivity().getId()).isEqualTo(fixture.activity().getId());
        assertThat(persistedActivity.getActiveEnrollmentCount()).isZero();
        assertThat(enrollmentActivityDslRepository.countActiveByActivityId(fixture.activity().getId())).isZero();
        assertThat(enrollmentActivityRepository.findNonCanceledActivityIdsByEnrollmentId(fixture.enrollment().getId()))
                .doesNotContain(fixture.activity().getId());
        assertThat(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, fixture.activity().getId()))
                .get().extracting(EnrollmentActivity::isCanceled).isEqualTo(true);
        assertThat(persistedEventEnrollment.getStatus()).isEqualTo(EnumEnrollmentState.CHECKED_IN);
        assertThat(eventActivityDslRepository.findAllCheckedInByUserAndEvent(userId, fixture.event().getId()))
                .extracting(activityDto -> activityDto.id()).doesNotContain(fixture.activity().getId());
        assertThat(eventActivityDslRepository.sumWorkloadHoursByCheckIn(userId, fixture.event().getId()))
                .isZero();

        persisted.restore();
        persistedActivity.registerActiveEnrollment();
        enrollmentActivityRepository.save(persisted);
        activityRepository.saveAndFlush(persistedActivity);
        enrollmentActivityRepository.flush();
        entityManager.clear();

        assertThat(enrollmentActivityRepository
                .findByEnrollmentIdAndActivityId(fixture.enrollment().getId(), fixture.activity().getId()))
                .get().extracting(EnrollmentActivity::isCanceled).isEqualTo(false);
        assertThat(enrollmentActivityDslRepository.countActiveByActivityId(fixture.activity().getId())).isEqualTo(1);
        assertThat(activityRepository.findById(fixture.activity().getId()).orElseThrow().getActiveEnrollmentCount())
                .isEqualTo(1);
    }

    @Test
    void deletingParticipantUnsubscriptionRemovesOnlyActivityLinkAndReleasesCount() {
        Fixture fixture = persistFixture(UUID.randomUUID(), EnumEnrollmentState.CONFIRMED, false, null);

        EnrollmentActivity link = enrollmentActivityRepository
                .findByEnrollmentIdAndActivityId(fixture.enrollment().getId(), fixture.activity().getId())
                .orElseThrow();
        EventActivity activity = activityRepository.findById(fixture.activity().getId()).orElseThrow();
        enrollmentActivityRepository.delete(link);
        activity.releaseActiveEnrollment();
        activityRepository.saveAndFlush(activity);
        enrollmentActivityRepository.flush();
        entityManager.clear();

        assertThat(enrollmentActivityRepository
                .findByEnrollmentIdAndActivityId(fixture.enrollment().getId(), fixture.activity().getId()))
                .isEmpty();
        assertThat(enrollmentRepository.findById(fixture.enrollment().getId())).isPresent();
        assertThat(activityRepository.findById(fixture.activity().getId())).isPresent();
        assertThat(activityRepository.findById(fixture.activity().getId()).orElseThrow().getActiveEnrollmentCount())
                .isZero();
        assertThat(enrollmentActivityDslRepository.countActiveByActivityId(fixture.activity().getId())).isZero();
    }

    private Fixture persistFixture(UUID userId, EnumEnrollmentState enrollmentState,
                                   boolean attended, LocalDateTime attendedAt) {
        Event event = eventRepository.saveAndFlush(Event.builder()
                .title("Evento de teste")
                .slug("evento-cancelamento-" + UUID.randomUUID())
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .status(EnumEventStatus.PUBLISHED)
                .ownerId(UUID.randomUUID())
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().minusDays(1))
                .build());
        EventActivity activity = activityRepository.saveAndFlush(EventActivity.builder()
                .event(event)
                .title("Atividade de teste")
                .type(EnumActivityType.LECTURE)
                .registrationPolicy(EnumActivityRegistrationPolicy.EVENT_REGISTRANTS_ONLY)
                .createdAt(LocalDateTime.now())
                .activeEnrollmentCount(1)
                .build());
        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.builder()
                .event(event)
                .userId(userId)
                .status(enrollmentState)
                .build());
        EnrollmentActivity link = EnrollmentActivity.builder()
                .enrollment(enrollment)
                .activity(activity)
                .attendedAt(attended ? attendedAt : null)
                .build();
        enrollmentActivityRepository.saveAndFlush(link);
        return new Fixture(event, activity, enrollment);
    }

    private record Fixture(Event event, EventActivity activity, Enrollment enrollment) {}
}

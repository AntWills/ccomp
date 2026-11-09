package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCursor;
import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, EventNotificationDslRepository.class,
        EventNotificationRecipientDslRepository.class, EventNotificationOutboxDslRepository.class})
class EventNotificationDslRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired EventNotificationDslRepository dslRepository;
    @Autowired EventNotificationRecipientDslRepository recipientDslRepository;
    @Autowired EventNotificationOutboxDslRepository outboxDslRepository;

    @Test
    void findsOnlyActiveEventAndActivityRecipientsWithCursorPagination() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Event event = event("notifications-event", now);
        entityManager.persist(event);
        EventActivity activity = activity(event, now);
        EventActivity otherActivity = activity(event, now);
        entityManager.persist(activity);
        entityManager.persist(otherActivity);

        UserModel sender = user("sender", now);
        UserModel eventUser = user("event-user", now);
        UserModel activityUser = user("activity-user", now);
        UserModel canceledUser = user("canceled-user", now);
        entityManager.persist(sender);
        entityManager.persist(eventUser);
        entityManager.persist(activityUser);
        entityManager.persist(canceledUser);

        Enrollment eventEnrollment = enrollment(event, eventUser, EnumEnrollmentState.CONFIRMED, now.minusHours(2));
        Enrollment activityEnrollment = enrollment(event, activityUser, EnumEnrollmentState.CHECKED_IN, now.minusHours(1));
        Enrollment canceledEnrollment = enrollment(event, canceledUser, EnumEnrollmentState.CANCELED, now);
        entityManager.persist(eventEnrollment);
        entityManager.persist(activityEnrollment);
        entityManager.persist(canceledEnrollment);
        entityManager.flush();

        entityManager.persist(new EnrollmentActivity(activityEnrollment, activity));
        entityManager.persist(new EnrollmentActivity(activityEnrollment, otherActivity));
        entityManager.flush();
        entityManager.clear();

        var eventRecipients = dslRepository.findEventRecipients(event.getId(), null, 10);
        assertThat(eventRecipients).extracting(candidate -> candidate.userName())
                .containsExactlyInAnyOrder("event-user", "activity-user");
        var activityRecipients = dslRepository.findActivityRecipients(event.getId(), activity.getId(), null, 10);
        assertThat(activityRecipients).extracting(candidate -> candidate.userName())
                .containsExactly("activity-user");
        assertThat(dslRepository.findSelectedRecipients(event.getId(), null,
                List.of(activityUser.getId(), canceledUser.getId())))
                .extracting(candidate -> candidate.userId()).containsExactly(activityUser.getId());
        assertThat(dslRepository.findSelectedRecipients(event.getId(), activity.getId(),
                List.of(activityUser.getId(), eventUser.getId())))
                .extracting(candidate -> candidate.userId()).containsExactly(activityUser.getId());

        var first = dslRepository.findEventRecipients(event.getId(), null, 1);
        assertThat(first).hasSize(1);
        var firstCandidate = first.getFirst();
        var next = dslRepository.findEventRecipients(event.getId(),
                new NotificationRecipientCursor(firstCandidate.createdAt(), firstCandidate.cursorId()), 10);
        assertThat(next).hasSize(1);
        assertThat(next.getFirst().userId()).isNotEqualTo(firstCandidate.userId());
    }

    @Test
    void listsSentAndPersonalNotificationsAndUpdatesReadAndClaimState() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Event event = event("notifications-history", now);
        entityManager.persist(event);
        UserModel sender = user("notification-sender", now);
        UserModel reader = user("notification-reader", now);
        entityManager.persist(sender);
        entityManager.persist(reader);
        entityManager.flush();

        EventNotification older = notification(event, sender, now.minusHours(1));
        EventNotification newer = notification(event, sender, now);
        entityManager.persist(older);
        entityManager.persist(newer);
        entityManager.flush();
        EventNotificationOutbox pendingOne = EventNotificationOutbox.builder()
                .notificationId(older.getId()).messageType("DISPATCH").build();
        EventNotificationOutbox pendingTwo = EventNotificationOutbox.builder()
                .notificationId(newer.getId()).messageType("DELIVERY").recipientIds("10,11").build();
        EventNotificationOutbox published = EventNotificationOutbox.builder()
                .notificationId(newer.getId()).messageType("DISPATCH").publishedAt(now).build();
        entityManager.persist(pendingOne);
        entityManager.persist(pendingTwo);
        entityManager.persist(published);
        entityManager.flush();
        EventNotificationRecipient unread = recipient(newer, reader, now);
        unread.setEmailStatus(NotificationEmailStatus.PENDING);
        EventNotificationRecipient read = recipient(older, reader, now.minusHours(1));
        read.setReadAt(now.minusMinutes(10));
        entityManager.persist(unread);
        entityManager.persist(read);
        entityManager.flush();
        entityManager.clear();

        assertThat(dslRepository.findSentByEvent(event.getId(), null, 10))
                .extracting(item -> item.id()).containsExactly(newer.getId(), older.getId());
        assertThat(outboxDslRepository.findPendingForPublish())
                .extracting(EventNotificationOutbox::getId)
                .containsExactly(pendingOne.getId(), pendingTwo.getId());
        var page = dslRepository.findForUser(reader.getId(), true, null, 10);
        assertThat(page).hasSize(1);
        assertThat(page.getFirst().notificationId()).isEqualTo(newer.getId());
        assertThat(page.getFirst().unread()).isTrue();

        assertThat(recipientDslRepository.markAllRead(reader.getId(), now)).isEqualTo(1);
        assertThat(recipientDslRepository.claimPending(unread.getId(), now)).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        EventNotificationRecipient updated = entityManager.find(EventNotificationRecipient.class, unread.getId());
        assertThat(updated.getReadAt()).isEqualTo(now);
        assertThat(updated.getEmailStatus()).isEqualTo(NotificationEmailStatus.PROCESSING);
        assertThat(updated.getEmailAttempts()).isEqualTo(2);
    }

    private Event event(String slug, LocalDateTime now) {
        return Event.builder().title("Evento notificações").slug(slug)
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL).format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID()).createdAt(now).build();
    }

    private EventActivity activity(Event event, LocalDateTime now) {
        return EventActivity.builder().title("Atividade").type(EnumActivityType.LECTURE).displayOrder(0L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC).createdAt(now).event(event).build();
    }

    private UserModel user(String username, LocalDateTime now) {
        return UserModel.builder().name(username).emailAddress(new EmailAddress(username + "@example.org"))
                .password("hash").statusAccount(EnumUserStatusAccount.ACTIVE).createdAt(now).updatedAt(now).build();
    }

    private Enrollment enrollment(Event event, UserModel user, EnumEnrollmentState status, LocalDateTime createdAt) {
        return Enrollment.builder().event(event).userId(user.getId()).status(status).createdAt(createdAt).build();
    }

    private EventNotification notification(Event event, UserModel sender, LocalDateTime createdAt) {
        return EventNotification.builder().eventId(event.getId()).eventTitle(event.getTitle())
                .senderUserId(sender.getId()).audience(NotificationAudience.EVENT).title("Atualização")
                .message("Informação importante").status(NotificationStatus.COMPLETED).recipientCount(1)
                .createdAt(createdAt).build();
    }

    private EventNotificationRecipient recipient(EventNotification notification, UserModel user,
            LocalDateTime createdAt) {
        return EventNotificationRecipient.builder().notificationId(notification.getId()).userId(user.getId())
                .userName(user.getName()).emailAddress(user.getEmailAddress().getValue())
                .emailStatus(NotificationEmailStatus.SENT).emailAttempts(1).emailSentAt(createdAt)
                .createdAt(createdAt).build();
    }
}

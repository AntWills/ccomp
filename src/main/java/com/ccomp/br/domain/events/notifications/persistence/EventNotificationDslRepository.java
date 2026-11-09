package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.dto.EventNotificationCursor;
import com.ccomp.br.domain.events.notifications.dto.EventNotificationListItem;
import com.ccomp.br.domain.events.notifications.dto.MyEventNotificationItem;
import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCandidate;
import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCursor;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ccomp.br.domain.events.notifications.persistence.QEventNotification.eventNotification;
import static com.ccomp.br.domain.events.notifications.persistence.QEventNotificationRecipient.eventNotificationRecipient;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollmentActivity.enrollmentActivity;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EventNotificationDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventNotificationDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Transactional(readOnly = true)
    public List<NotificationRecipientCandidate> findEventRecipients(Long eventId,
            @Nullable NotificationRecipientCursor cursor, int limit) {
        BooleanBuilder where = activeEnrollment(eventId);
        addCursor(where, enrollment.createdAt, enrollment.id, cursor);
        return queryFactory.select(Projections.constructor(
                        NotificationRecipientCandidate.class,
                        userModel.id, userModel.name, userModel.emailAddress.value,
                        enrollment.createdAt, enrollment.id))
                .from(enrollment).join(userModel).on(userModel.id.eq(enrollment.userId))
                .where(where).orderBy(enrollment.createdAt.desc(), enrollment.id.desc()).limit(limit).fetch();
    }

    @Transactional(readOnly = true)
    public List<NotificationRecipientCandidate> findActivityRecipients(Long eventId, Long activityId,
            @Nullable NotificationRecipientCursor cursor, int limit) {
        BooleanBuilder where = activeEnrollment(eventId);
        where.and(enrollmentActivity.activity.id.eq(activityId));
        addCursor(where, enrollmentActivity.createdAt, enrollmentActivity.id, cursor);
        return queryFactory.select(Projections.constructor(
                        NotificationRecipientCandidate.class,
                        userModel.id, userModel.name, userModel.emailAddress.value,
                        enrollmentActivity.createdAt, enrollmentActivity.id))
                .from(enrollmentActivity).join(enrollmentActivity.enrollment, enrollment)
                .join(userModel).on(userModel.id.eq(enrollment.userId))
                .where(where).orderBy(enrollmentActivity.createdAt.desc(), enrollmentActivity.id.desc())
                .limit(limit).fetch();
    }

    @Transactional(readOnly = true)
    public List<NotificationRecipientCandidate> findSelectedRecipients(Long eventId, @Nullable Long activityId,
            List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        BooleanBuilder where = activeEnrollment(eventId).and(enrollment.userId.in(userIds));
        if (activityId == null) {
            return queryFactory.select(Projections.constructor(
                            NotificationRecipientCandidate.class,
                            userModel.id, userModel.name, userModel.emailAddress.value,
                            enrollment.createdAt, enrollment.id))
                    .from(enrollment).join(userModel).on(userModel.id.eq(enrollment.userId))
                    .where(where).orderBy(enrollment.createdAt.desc(), enrollment.id.desc()).fetch();
        }
        where.and(enrollmentActivity.activity.id.eq(activityId));
        return queryFactory.select(Projections.constructor(
                        NotificationRecipientCandidate.class,
                        userModel.id, userModel.name, userModel.emailAddress.value,
                        enrollmentActivity.createdAt, enrollmentActivity.id))
                .from(enrollmentActivity).join(enrollmentActivity.enrollment, enrollment)
                .join(userModel).on(userModel.id.eq(enrollment.userId))
                .where(where).orderBy(enrollmentActivity.createdAt.desc(), enrollmentActivity.id.desc()).fetch();
    }

    @Transactional(readOnly = true)
    public List<EventNotificationListItem> findSentByEvent(Long eventId,
            @Nullable EventNotificationCursor cursor, int limit) {
        BooleanBuilder where = new BooleanBuilder(eventNotification.eventId.eq(eventId));
        addCursor(where, eventNotification.createdAt, eventNotification.id, cursor);
        return queryFactory.select(Projections.constructor(
                        EventNotificationListItem.class,
                        eventNotification.id, eventNotification.eventId, eventNotification.activityId,
                        eventNotification.senderUserId, userModel.name, eventNotification.audience,
                        eventNotification.title, eventNotification.message, eventNotification.status,
                        eventNotification.recipientCount,
                        eventNotification.createdAt))
                .from(eventNotification)
                .join(userModel).on(userModel.id.eq(eventNotification.senderUserId))
                .where(where)
                .orderBy(eventNotification.createdAt.desc(), eventNotification.id.desc())
                .limit(limit).fetch();
    }

    @Transactional(readOnly = true)
    public List<MyEventNotificationItem> findForUser(UUID userId, boolean unreadOnly,
            @Nullable EventNotificationCursor cursor, int limit) {
        BooleanBuilder where = new BooleanBuilder(eventNotificationRecipient.userId.eq(userId));
        if (unreadOnly) {
            where.and(eventNotificationRecipient.readAt.isNull());
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            where.and(eventNotificationRecipient.createdAt.lt(cursor.createdAt())
                    .or(eventNotificationRecipient.createdAt.eq(cursor.createdAt())
                            .and(eventNotificationRecipient.id.lt(cursor.id()))));
        }
        return queryFactory.select(Projections.constructor(
                        MyEventNotificationItem.class,
                        eventNotificationRecipient.id, eventNotification.id,
                        eventNotification.eventId, eventNotification.activityId,
                        eventNotification.title, eventNotification.message, eventNotification.senderUserId,
                        userModel.name,
                        eventNotificationRecipient.createdAt, eventNotificationRecipient.readAt,
                        eventNotificationRecipient.readAt.isNull()))
                .from(eventNotificationRecipient).join(eventNotification)
                .on(eventNotification.id.eq(eventNotificationRecipient.notificationId))
                .join(userModel).on(userModel.id.eq(eventNotification.senderUserId))
                .where(where)
                .orderBy(eventNotificationRecipient.createdAt.desc(), eventNotificationRecipient.id.desc())
                .limit(limit).fetch();
    }

    private BooleanBuilder activeEnrollment(Long eventId) {
        return new BooleanBuilder(
                enrollment.event.id.eq(eventId)
                        .and(enrollment.status.in(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN))
        );
    }

    private void addCursor(
            BooleanBuilder where,
            DateTimePath<LocalDateTime> createdAt,
            NumberPath<Long> id,
            @Nullable NotificationRecipientCursor cursor
    ) {
        if (cursor == null || cursor.id() == null || cursor.createdAt() == null) {
            return;
        }

        where.and(cursorCondition(createdAt, id, cursor.createdAt(), cursor.id()));
    }

    private void addCursor(
            BooleanBuilder where,
            DateTimePath<LocalDateTime> createdAt,
            NumberPath<Long> id,
            @Nullable EventNotificationCursor cursor
    ) {
        if (cursor == null || cursor.id() == null || cursor.createdAt() == null) {
            return;
        }

        where.and(cursorCondition(createdAt, id, cursor.createdAt(), cursor.id()));
    }

    private BooleanExpression cursorCondition(
            DateTimePath<LocalDateTime> createdAt,
            NumberPath<Long> id,
            LocalDateTime cursorCreatedAt,
            Long cursorId
    ) {
        return createdAt.lt(cursorCreatedAt)
                .or(createdAt.eq(cursorCreatedAt).and(id.lt(cursorId)));
    }
}

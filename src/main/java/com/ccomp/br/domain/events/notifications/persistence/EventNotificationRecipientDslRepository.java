package com.ccomp.br.domain.events.notifications.persistence;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ccomp.br.domain.events.notifications.persistence.QEventNotificationRecipient.eventNotificationRecipient;

@Repository
public class EventNotificationRecipientDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventNotificationRecipientDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Transactional
    public int markAllRead(UUID userId, LocalDateTime readAt) {
        return Math.toIntExact(queryFactory.update(eventNotificationRecipient)
                .set(eventNotificationRecipient.readAt, readAt)
                .where(eventNotificationRecipient.userId.eq(userId), eventNotificationRecipient.readAt.isNull())
                .execute());
    }

    @Transactional
    public int claimPending(Long recipientId, LocalDateTime attemptAt) {
        return Math.toIntExact(queryFactory.update(eventNotificationRecipient)
                .set(eventNotificationRecipient.emailStatus, NotificationEmailStatus.PROCESSING)
                .set(eventNotificationRecipient.emailAttempts, eventNotificationRecipient.emailAttempts.add(1))
                .set(eventNotificationRecipient.lastEmailAttemptAt, attemptAt)
                .where(eventNotificationRecipient.id.eq(recipientId)
                        .and(eventNotificationRecipient.emailStatus.eq(NotificationEmailStatus.PENDING)))
                .execute());
    }
}

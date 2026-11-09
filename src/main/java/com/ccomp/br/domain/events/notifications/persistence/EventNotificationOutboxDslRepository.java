package com.ccomp.br.domain.events.notifications.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ccomp.br.domain.events.notifications.persistence.QEventNotificationOutbox.eventNotificationOutbox;

@Repository
public class EventNotificationOutboxDslRepository {
    private static final int PUBLISH_BATCH_SIZE = 100;
    private final JPAQueryFactory queryFactory;

    public EventNotificationOutboxDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Transactional(readOnly = true)
    public List<EventNotificationOutbox> findPendingForPublish() {
        return queryFactory.selectFrom(eventNotificationOutbox)
                .where(eventNotificationOutbox.publishedAt.isNull())
                .orderBy(eventNotificationOutbox.id.asc())
                .limit(PUBLISH_BATCH_SIZE)
                .fetch();
    }
}

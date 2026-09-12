package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.ccomp.br.domain.events.activities.persistence.QEventActivity.eventActivity;
import static com.ccomp.br.domain.events.core.persistence.QEvent.event;

@Repository
public class EventActivityDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventActivityDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<Event> findEventByActivityId(Long activityId) {
        Event result = queryFactory
                .select(event)
                .from(event)
                .join(event.activities, eventActivity)
                .where(eventActivity.id.eq(activityId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}

package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.persistence.Event;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import static com.ccomp.br.domain.events.persistence.QEvent.event;
import static com.ccomp.br.domain.events.persistence.activities.QEventActivity.eventActivity;

import java.util.Optional;

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

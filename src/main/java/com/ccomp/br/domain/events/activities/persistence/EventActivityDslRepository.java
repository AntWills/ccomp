package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ccomp.br.domain.events.activities.persistence.QEventActivity.eventActivity;
import static com.ccomp.br.domain.events.core.persistence.QEvent.event;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollmentActivity.enrollmentActivity;

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

    public List<EventActivityDTO> findAllByEventIdWithCursor(
            Long eventId, @Nullable EventActivityCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventActivity.event.id.eq(eventId));

        if (cursor != null && cursor.displayOrder() != null && cursor.id() != null) {
            BooleanExpression greaterDisplayOrder = eventActivity.displayOrder.gt(cursor.displayOrder());
            BooleanExpression sameDisplayOrderSmallerId = eventActivity.displayOrder.eq(cursor.displayOrder())
                    .and(eventActivity.id.lt(cursor.id()));

            whereClause.and(greaterDisplayOrder.or(sameDisplayOrderSmallerId));
        }

        return queryFactory.select(
                Projections.constructor(
                        EventActivityDTO.class,
                        eventActivity.id,
                        eventActivity.event.id,
                        eventActivity.title,
                        eventActivity.description,
                        eventActivity.displayOrder,
                        eventActivity.location,
                        eventActivity.startDate,
                        eventActivity.endDate,
                        eventActivity.registrationPolicy,
                        eventActivity.type,
                        eventActivity.createdAt
                    )
                )
                .from(eventActivity)
                .where(whereClause)
                .orderBy(
                        eventActivity.displayOrder.asc(),
                        eventActivity.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventActivityDTO> findSubscriptionsWithCursor(
            UUID userId, Long eventId, @Nullable EventActivityCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventActivity.event.id.eq(eventId));
        whereClause.and(
                enrollment.status.in(
                        EnumEnrollmentState.CONFIRMED,
                        EnumEnrollmentState.CHECKED_IN
                )
        );
        whereClause.and(enrollmentActivity.enrollment.userId.eq(userId));

        if (cursor != null && cursor.displayOrder() != null && cursor.id() != null) {
            BooleanExpression greaterDisplayOrder = eventActivity.displayOrder.gt(cursor.displayOrder());
            BooleanExpression sameDisplayOrderSmallerId = eventActivity.displayOrder.eq(cursor.displayOrder())
                    .and(eventActivity.id.lt(cursor.id()));

            whereClause.and(greaterDisplayOrder.or(sameDisplayOrderSmallerId));
        }

        return queryFactory.select(
                        Projections.constructor(
                                EventActivityDTO.class,
                                eventActivity.id,
                                eventActivity.event.id,
                                eventActivity.title,
                                eventActivity.description,
                                eventActivity.displayOrder,
                                eventActivity.location,
                                eventActivity.startDate,
                                eventActivity.endDate,
                                eventActivity.registrationPolicy,
                                eventActivity.type,
                                eventActivity.createdAt
                        )
                )
                .from(eventActivity)
                .innerJoin(eventActivity.enrollmentsActivities, enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(whereClause)
                .orderBy(
                        eventActivity.displayOrder.asc(),
                        eventActivity.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}

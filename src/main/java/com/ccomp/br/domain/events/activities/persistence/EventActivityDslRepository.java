package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.domain.events.activities.dto.EventActivityConflictCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
                        eventActivity.workloadHours,
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
                                eventActivity.workloadHours,
                                eventActivity.registrationPolicy,
                                eventActivity.type,
                                eventActivity.createdAt
                        )
                )
                .from(eventActivity)
                .innerJoin(eventActivity.enrollments, enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(whereClause)
                .orderBy(
                        eventActivity.displayOrder.asc(),
                        eventActivity.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public boolean existsSchedulingConflict(UUID userId, EventActivity activity) {
        if (activity.getStartDate() == null || activity.getEndDate() == null)
            return false;

        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(enrollmentActivity.enrollment.userId.eq(userId));
        whereClause.and(
                enrollmentActivity.enrollment.status.in(
                        EnumEnrollmentState.CONFIRMED,
                        EnumEnrollmentState.CHECKED_IN
                )
        );

        // Condição de sobreposição de horários: inicioA < fimB E fimA > inicioB
        whereClause.and(eventActivity.startDate.lt(activity.getEndDate()))
                .and(eventActivity.endDate.gt(activity.getStartDate()));

        whereClause.and(eventActivity.id.ne(activity.getId()));

        Integer result = queryFactory
                .selectOne()
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.activity, eventActivity)
                .where(whereClause)
                .fetchFirst();

        return result != null;
    }

    public List<EventActivityDTO> findConflictingActivities(
            UUID userId, EventActivity activity, @Nullable EventActivityConflictCursor cursor, int limit) {
        if (activity.getStartDate() == null || activity.getEndDate() == null) {
            return List.of();
        }

        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(enrollmentActivity.enrollment.userId.eq(userId));
        whereClause.and(
                enrollmentActivity.enrollment.status.in(
                        EnumEnrollmentState.CONFIRMED,
                        EnumEnrollmentState.CHECKED_IN
                )
        );

        whereClause.and(eventActivity.startDate.lt(activity.getEndDate()))
                .and(eventActivity.endDate.gt(activity.getStartDate()));

        whereClause.and(eventActivity.id.ne(activity.getId()));

        // Cursor composto: startDate crescente (gt) com desempate por id decrescente (lt)
        if (cursor != null && cursor.startDate() != null && cursor.id() != null) {
            BooleanExpression greaterStartDate = eventActivity.startDate.gt(cursor.startDate());
            BooleanExpression sameStartDateSmallerId = eventActivity.startDate.eq(cursor.startDate())
                    .and(eventActivity.id.lt(cursor.id()));

            whereClause.and(greaterStartDate.or(sameStartDateSmallerId));
        }

        return queryFactory
                .select(
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
                                eventActivity.workloadHours,
                                eventActivity.registrationPolicy,
                                eventActivity.type,
                                eventActivity.createdAt
                        )
                )
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.activity, eventActivity)
                .where(whereClause)
                .orderBy(
                        eventActivity.startDate.asc(),
                        eventActivity.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    /**
     * Carrega todas as atividades do evento em que o usuário realmente participou
     * (inscrição com check-in feito), ordenadas por ordem de exibição e id.
     */
    public List<EventActivityDTO> findAllCheckedInByUserAndEvent(UUID userId, Long eventId) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventActivity.event.id.eq(eventId));
        whereClause.and(enrollmentActivity.enrollment.userId.eq(userId));
        whereClause.and(enrollmentActivity.attendedAt.isNotNull());

        return queryFactory
                .select(
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
                                eventActivity.workloadHours,
                                eventActivity.registrationPolicy,
                                eventActivity.type,
                                eventActivity.createdAt
                        )
                )
                .from(eventActivity)
                .innerJoin(eventActivity.enrollments, enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(whereClause)
                .orderBy(
                        eventActivity.displayOrder.asc(),
                        eventActivity.id.desc()
                )
                .fetch();
    }

    public BigDecimal sumWorkloadHoursByCheckIn(UUID userId, Long eventId) {
        BigDecimal result = queryFactory
                .select(Expressions.numberTemplate(
                        BigDecimal.class,
                        "coalesce(sum({0}), 0)",
                        eventActivity.workloadHours))
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.activity, eventActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(
                        enrollment.userId.eq(userId),
                        eventActivity.event.id.eq(eventId),
                        enrollmentActivity.attendedAt.isNotNull()
                )
                .fetchOne();

        return result != null ? result : BigDecimal.ZERO;
    }
}

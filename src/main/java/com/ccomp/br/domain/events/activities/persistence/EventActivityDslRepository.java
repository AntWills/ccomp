package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.domain.events.activities.dto.EventActivityConflictCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
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
import static com.ccomp.br.domain.events.instructor.persistence.QEventInstructor.eventInstructor;

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
                        eventActivity.createdAt,
                        eventActivity.event.timeZone,
                        eventActivity.maxCapacity
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

    public List<ActivityDTO> findSubscriptionsWithCursor(
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
                                ActivityDTO.class,
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
                                eventActivity.createdAt,
                                eventActivity.event.timeZone,
                                eventActivity.maxCapacity
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
        // Horários são locais ao evento; atividades de eventos distintos não conflitam.
        whereClause.and(eventActivity.event.id.eq(activity.getEvent().getId()));

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

    public List<ActivityDTO> findConflictingActivities(
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

        // Mantém a consulta de conflitos consistente com existsSchedulingConflict.
        whereClause.and(eventActivity.event.id.eq(activity.getEvent().getId()));

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
                                ActivityDTO.class,
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
                                eventActivity.createdAt,
                                eventActivity.event.timeZone,
                                eventActivity.maxCapacity
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

    public List<ActivityDTO> findAllConflictingActivities(UUID userId, EventActivity activity) {
        return findConflictingActivities(userId, activity, null, Integer.MAX_VALUE);
    }

    /**
     * Carrega todas as atividades do evento em que o usuário realmente participou
     * (inscrição com check-in feito), ordenadas por ordem de exibição e id.
     */
    public List<ActivityDTO> findAllCheckedInByUserAndEvent(UUID userId, Long eventId) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventActivity.event.id.eq(eventId));
        whereClause.and(enrollmentActivity.enrollment.userId.eq(userId));
        whereClause.and(enrollmentActivity.attendedAt.isNotNull());

        return queryFactory
                .select(
                        Projections.constructor(
                                ActivityDTO.class,
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
                                eventActivity.createdAt,
                                eventActivity.event.timeZone,
                                eventActivity.maxCapacity
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

    public BigDecimal sumWorkloadHoursByInstructor(UUID userId, Long eventId) {
        BigDecimal result = queryFactory
                .select(Expressions.numberTemplate(BigDecimal.class, "coalesce(sum({0}), 0)", eventActivity.workloadHours))
                .from(eventActivity)
                .join(eventInstructor).on(eventInstructor.activityId.eq(eventActivity.id))
                .where(eventInstructor.userId.eq(userId),
                        eventInstructor.status.eq(EnumInstructorStatus.CONFIRMED),
                        eventActivity.eventId.eq(eventId))
                .fetchOne();
        return result == null ? BigDecimal.ZERO : result;
    }

    public Optional<UUID> findConfirmedInstructorAccount(Long instructorId, Long eventId) {
        UUID userId = queryFactory.select(eventInstructor.userId)
                .from(eventActivity)
                .join(eventInstructor).on(eventInstructor.activityId.eq(eventActivity.id))
                .where(eventInstructor.id.eq(instructorId),
                        eventInstructor.status.eq(EnumInstructorStatus.CONFIRMED),
                        eventInstructor.userId.isNotNull(),
                        eventActivity.eventId.eq(eventId))
                .fetchFirst();
        return Optional.ofNullable(userId);
    }
}

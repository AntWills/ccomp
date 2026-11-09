package com.ccomp.br.domain.events.guests.persistence.dsl;

import java.util.List;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.ActivityGuestDTO;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.instructor.dto.ActivityParticipantDTO;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import static com.ccomp.br.domain.events.guests.persistence.QActivityGuest.activityGuest;
import static com.ccomp.br.domain.events.activities.persistence.QEventActivity.eventActivity;

@Repository
public class ActivityGuestDslRepository {
    private final JPAQueryFactory queryFactory;

    public ActivityGuestDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ActivityDTO> findActivitiesByEventGuestId(Long eventGuestId) {
        return queryFactory
                .select(Projections.constructor(
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
                ))
                .from(activityGuest)
                .innerJoin(activityGuest.activity, eventActivity)
                .where(activityGuest.eventGuest.id.eq(eventGuestId))
                .orderBy(eventActivity.displayOrder.asc(), eventActivity.id.desc())
                .fetch();
    }

    public List<ActivityGuestDTO> findActivityGuestsWithCursor(
            Long activityId, boolean isManager, GuestCursor cursor, int limit) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(activityGuest.activity.id.eq(activityId));

        // Como a regra de negócio está no EventGuest, acessamos a relação com o join automático
        if (!isManager) {
            where.and(activityGuest.eventGuest.visibility.eq(EnumGuestVisibility.PUBLIC))
                    .and(activityGuest.eventGuest.status.eq(EnumGuestStatus.CONFIRMED));
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    activityGuest.createdAt, activityGuest.id,
                    cursor.createdAt(), cursor.id()
            );
            where.and(cursorCondition);
        }

        return queryFactory
                .select(Projections.constructor(
                        ActivityGuestDTO.class,
                        activityGuest.id,
                        activityGuest.activity.id,
                        activityGuest.eventGuest.id,
                        activityGuest.eventGuest.name,
                        activityGuest.eventGuest.description,
                        activityGuest.eventGuest.imageKey,
                        activityGuest.role,
                        activityGuest.eventGuest.status,
                        activityGuest.eventGuest.visibility,
                        Expressions.asBoolean(true),
                        activityGuest.createdAt
                ))
                .from(activityGuest)
                .innerJoin(activityGuest.eventGuest)
                .where(where)
                .orderBy(activityGuest.createdAt.desc(), activityGuest.id.desc())
                .limit(limit)
                .fetch();
    }

    public List<ActivityParticipantDTO> findActivityGuestsForParticipants(Long activityId, boolean isManager) {
        BooleanBuilder where = new BooleanBuilder().and(activityGuest.activity.id.eq(activityId));
        if (!isManager) {
            where.and(activityGuest.eventGuest.visibility.eq(EnumGuestVisibility.PUBLIC))
                    .and(activityGuest.eventGuest.status.eq(EnumGuestStatus.CONFIRMED));
        }

        return queryFactory
                .select(Projections.constructor(
                        ActivityParticipantDTO.class,
                        activityGuest.id,
                        Expressions.nullExpression(UUID.class),
                        activityGuest.activity.id,
                        new CaseBuilder()
                                .when(activityGuest.eventGuest.status.eq(EnumGuestStatus.CONFIRMED))
                                .then(EnumInstructorStatus.CONFIRMED.name())
                                .otherwise(EnumInstructorStatus.CANCELED.name()),
                        activityGuest.eventGuest.name,
                        activityGuest.eventGuest.description,
                        activityGuest.role.stringValue(),
                        Expressions.asBoolean(true),
                        activityGuest.createdAt
                ))
                .from(activityGuest)
                .innerJoin(activityGuest.eventGuest)
                .where(where)
                .orderBy(activityGuest.createdAt.desc(), activityGuest.id.desc())
                .fetch();
    }
}

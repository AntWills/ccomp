package com.ccomp.br.domain.events.instructor.persistence;

import java.util.List;

import com.ccomp.br.domain.events.activities.persistence.QEventActivity;
import com.ccomp.br.domain.events.instructor.dto.ActivityParticipantDTO;
import com.ccomp.br.domain.events.instructor.dto.EventInstructorDTO;
import com.ccomp.br.domain.events.instructor.dto.InstructorCursor;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import static com.ccomp.br.domain.events.instructor.persistence.QEventInstructor.eventInstructor;

@Repository
public class InstructorDslRepository {
    private final JPAQueryFactory queryFactory;

    public InstructorDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventInstructorDTO> findByEvent(
            Long eventId,
            InstructorCursor cursor,
            int limit
    ) {
        QEventActivity activity = new QEventActivity("activity");
        BooleanExpression condition = activity.eventId.eq(eventId)
                .and(eventInstructor.status.eq(EnumInstructorStatus.CONFIRMED));

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            condition = condition.and(Expressions.booleanTemplate(
                    "({0}, {1}) < ({2}, {3})",
                    eventInstructor.createdAt,
                    eventInstructor.id,
                    cursor.createdAt(),
                    cursor.id()
            ));
        }

        return queryFactory
                .select(Projections.constructor(
                        EventInstructorDTO.class,
                        eventInstructor.id,
                        eventInstructor.userId,
                        eventInstructor.activityId,
                        eventInstructor.status,
                        eventInstructor.name,
                        eventInstructor.description,
                        eventInstructor.role,
                        Expressions.asBoolean(false),
                        eventInstructor.createdAt
                ))
                .from(eventInstructor)
                .join(activity)
                .on(activity.id.eq(eventInstructor.activityId))
                .where(condition)
                .orderBy(
                        eventInstructor.createdAt.desc(),
                        eventInstructor.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<ActivityParticipantDTO> findByActivity(Long activityId) {
        return queryFactory
                .select(Projections.constructor(
                        ActivityParticipantDTO.class,
                        eventInstructor.id,
                        eventInstructor.userId,
                        eventInstructor.activityId,
                        eventInstructor.status,
                        eventInstructor.name,
                        eventInstructor.description,
                        eventInstructor.role.stringValue(),
                        Expressions.asBoolean(false),
                        eventInstructor.createdAt
                ))
                .from(eventInstructor)
                .where(eventInstructor.activityId.eq(activityId)
                        .and(eventInstructor.status.eq(EnumInstructorStatus.CONFIRMED)))
                .orderBy(eventInstructor.createdAt.desc(), eventInstructor.id.desc())
                .fetch();
    }
}

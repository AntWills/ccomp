package com.ccomp.br.domain.events.instructor.persistence;

import com.ccomp.br.domain.events.activities.persistence.QEventActivity;
import com.ccomp.br.domain.events.instructor.dto.InstructorCursor;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.instructor.persistence.QEventInstructor.eventInstructor;

@Repository
public class InstructorDslRepository {
    private final JPAQueryFactory queryFactory;

    public InstructorDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventInstructor> findByEvent(
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
                .selectFrom(eventInstructor)
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
}

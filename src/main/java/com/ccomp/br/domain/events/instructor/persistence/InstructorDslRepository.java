package com.ccomp.br.domain.events.instructor.persistence;

import java.util.List;

import com.ccomp.br.domain.events.activities.persistence.QEventActivity;
import com.ccomp.br.domain.events.instructor.dto.ActivityParticipantDTO;
import com.ccomp.br.domain.events.instructor.dto.EventInstructorDTO;
import com.ccomp.br.domain.events.instructor.dto.InstructorCursor;
import com.ccomp.br.domain.events.instructor.dto.InstructorSearchResult;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.shared.utils.TextSearchDSL;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import static com.ccomp.br.domain.events.instructor.persistence.QEventInstructor.eventInstructor;

@Repository
public class InstructorDslRepository {
    private static final double MIN_SIMILARITY = 0.4;

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    public InstructorDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventInstructorDTO> findByEvent(
            Long eventId,
            InstructorCursor cursor,
            int limit
    ) {
        return searchByEvent(eventId, null, cursor, limit).stream()
                .map(InstructorSearchResult::instructor)
                .toList();
    }

    public List<InstructorSearchResult> searchByEvent(
            Long eventId,
            @Nullable String searchTerm,
            @Nullable InstructorCursor cursor,
            int limit
    ) {
        QEventActivity activity = new QEventActivity("activity");
        BooleanBuilder condition = new BooleanBuilder(activity.eventId.eq(eventId)
                .and(eventInstructor.status.eq(EnumInstructorStatus.CONFIRMED)));

        var relevance = searchTerm == null ? null
                : TextSearchDSL.bestScore(searchTerm, eventInstructor.name);
        if (searchTerm != null) {
            entityManager.createNativeQuery(
                            "select set_config('pg_trgm.word_similarity_threshold', '0.399999', true)")
                    .getSingleResult();
            condition.and(TextSearchDSL.matchAny(searchTerm, MIN_SIMILARITY, eventInstructor.name));
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            if (relevance != null && cursor.relevance() != null) {
                condition.and(Expressions.booleanTemplate(
                        "({0}, {1}, {2}) < ({3}, {4}, {5})",
                        relevance, eventInstructor.createdAt, eventInstructor.id,
                        Expressions.constant(cursor.relevance()),
                        Expressions.constant(cursor.createdAt()), Expressions.constant(cursor.id())
                ));
            } else {
                condition.and(Expressions.booleanTemplate(
                        "({0}, {1}) < ({2}, {3})",
                        eventInstructor.createdAt, eventInstructor.id,
                        Expressions.constant(cursor.createdAt()), Expressions.constant(cursor.id())
                ));
            }
        }

        var query = queryFactory
                .select(Projections.constructor(
                        InstructorSearchResult.class,
                        Projections.constructor(
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
                        ),
                        relevance == null ? Expressions.nullExpression(Double.class) : relevance
                ))
                .from(eventInstructor)
                .join(activity)
                .on(activity.id.eq(eventInstructor.activityId))
                .where(condition);
        if (relevance != null) {
            query.orderBy(relevance.desc(), eventInstructor.createdAt.desc(), eventInstructor.id.desc());
        } else {
            query.orderBy(eventInstructor.createdAt.desc(), eventInstructor.id.desc());
        }
        return query.limit(limit).fetch();
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

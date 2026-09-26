package com.ccomp.br.domain.events.core.persistence;

import com.ccomp.br.domain.events.core.dto.EventCursor;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.ccomp.br.domain.events.core.persistence.QEvent.event;
import static com.ccomp.br.domain.events.editors.persistence.QEventEditor.eventEditor;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;

@Repository
public class EventDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventListItemDTO> findByCursor(
            EventsFilterRequest filter,
            @Nullable EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(event.status.eq(EnumEventStatus.PUBLISHED));

        filter.categoryOpt().ifPresent(category ->
                whereClause.and(event.category.eq(category))
        );

        filter.formatOpt().ifPresent(format ->
                whereClause.and(event.format.eq(format))
        );

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    event.startDate,
                    event.id,
                    cursor.startDate(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageKey,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllByOwnerId(
            UUID ownerId,
            @Nullable EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(event.ownerId.eq(ownerId));

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    event.startDate,
                    event.id,
                    cursor.startDate(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageKey,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllSubscriptions(
            UUID participantId,
            @Nullable EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(enrollment.userId.eq(participantId));
        whereClause.and(
                enrollment.status.in(
                        EnumEnrollmentState.CONFIRMED,
                        EnumEnrollmentState.CHECKED_IN
                )
        );

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    event.startDate,
                    event.id,
                    cursor.startDate(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageKey,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .innerJoin(event.enrollments, enrollment)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllWhereUserIsEditor(
            UUID editorId,
            @Nullable EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventEditor.userId.eq(editorId));
        whereClause.and(eventEditor.status.eq(EnumEditorsStatus.ACTIVE));

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    event.startDate,
                    event.id,
                    cursor.startDate(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageKey,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .innerJoin(event.editors, eventEditor)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
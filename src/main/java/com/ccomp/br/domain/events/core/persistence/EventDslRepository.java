package com.ccomp.br.domain.events.core.persistence;

import com.ccomp.br.domain.events.core.dto.EventCursor;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.dto.EventSearchResult;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
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

    public List<EventSearchResult> findByCursor(
            EventsFilterRequest filter,
            @Nullable EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();
        var searchTerm = filter.searchTermOpt();
        NumberExpression<Double> relevance = searchTerm.map(this::relevanceExpression).orElse(null);

        whereClause.and(event.status.eq(EnumEventStatus.PUBLISHED));

        filter.categoryOpt().ifPresent(category ->
                whereClause.and(event.category.eq(category))
        );

        filter.formatOpt().ifPresent(format ->
                whereClause.and(event.format.eq(format))
        );

        searchTerm.ifPresent(term -> {
            var wordSimilarity = Expressions.numberTemplate(
                    Double.class,
                    "cast(function('word_similarity', {0}, {1}) as double)",
                    Expressions.constant(term),
                    event.title
            );
            whereClause.and(wordSimilarity.goe(0.4));
        });

        if (cursor != null && cursor.id() != null && cursor.startDate() != null
                && searchTerm.isPresent() && cursor.relevance() != null) {
            whereClause.and(Expressions.booleanTemplate(
                    "({0}, {1}, {2}) < ({3}, {4}, {5})",
                    relevance,
                    event.startDate,
                    event.id,
                    Expressions.constant(cursor.relevance()),
                    Expressions.constant(cursor.startDate()),
                    Expressions.constant(cursor.id())
            ));
        } else if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    event.startDate,
                    event.id,
                    cursor.startDate(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        var query = queryFactory
                .select(
                        Projections.constructor(
                                EventSearchResult.class,
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
                                ),
                                searchTerm.isPresent() ? relevance : Expressions.nullExpression(Double.class)
                        )
                )
                .from(event)
                .where(whereClause);

        if (searchTerm.isPresent()) {
            query.orderBy(relevance.desc(), event.startDate.desc(), event.id.desc());
        } else {
            query.orderBy(event.startDate.desc(), event.id.desc());
        }

        return query.limit(limit).fetch();
    }

    private NumberExpression<Double> relevanceExpression(String term) {
        var termExpression = Expressions.constant(term);
        var wordSimilarity = Expressions.numberTemplate(
                Double.class,
                "cast(function('word_similarity', {0}, {1}) as double)",
                termExpression,
                event.title
        );
        var similarity = Expressions.numberTemplate(
                Double.class,
                "cast(function('similarity', {0}, {1}) as double)",
                termExpression,
                event.title
        );
        return wordSimilarity.add(similarity);
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

package com.ccomp.br.domain.events.guests.persistence.dsl;

import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestSearchResult;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.shared.utils.TextSearchDSL;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.guests.persistence.QEventGuest.eventGuest;

@Repository
public class EventGuestDslRepository {
    private static final double MIN_SIMILARITY = 0.4;

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    public EventGuestDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventGuestDTO> findGuestsWithCursor(
            Long eventId, boolean isManager, GuestCursor cursor, int limit) {
        return searchGuestsWithCursor(eventId, isManager, null, cursor, limit).stream()
                .map(EventGuestSearchResult::guest)
                .toList();
    }

    public List<EventGuestSearchResult> searchGuestsWithCursor(
            Long eventId, boolean isManager, @Nullable String searchTerm, @Nullable GuestCursor cursor, int limit) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(eventGuest.event.id.eq(eventId));

        // Se não for gestor, filtra apenas os públicos e confirmados
        if (!isManager) {
            where.and(eventGuest.visibility.eq(EnumGuestVisibility.PUBLIC))
                    .and(eventGuest.status.eq(EnumGuestStatus.CONFIRMED));
        }

        var relevance = searchTerm == null ? null : TextSearchDSL.bestScore(searchTerm, eventGuest.name);
        if (searchTerm != null) {
            entityManager.createNativeQuery(
                            "select set_config('pg_trgm.word_similarity_threshold', '0.399999', true)")
                    .getSingleResult();
            where.and(TextSearchDSL.matchAny(searchTerm, MIN_SIMILARITY, eventGuest.name));
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            if (relevance != null && cursor.relevance() != null) {
                where.and(Expressions.booleanTemplate(
                        "({0}, {1}, {2}) < ({3}, {4}, {5})",
                        relevance, eventGuest.createdAt, eventGuest.id,
                        Expressions.constant(cursor.relevance()),
                        Expressions.constant(cursor.createdAt()), Expressions.constant(cursor.id())
                ));
            } else {
                BooleanExpression cursorCondition = Expressions.booleanTemplate(
                        "( {0}, {1} ) < ( {2}, {3} )",
                        eventGuest.createdAt, eventGuest.id,
                        Expressions.constant(cursor.createdAt()), Expressions.constant(cursor.id())
                );
                where.and(cursorCondition);
            }
        }

        var query = queryFactory
                .select(Projections.constructor(
                        EventGuestSearchResult.class,
                        Projections.constructor(
                                EventGuestDTO.class,
                                eventGuest.id,
                                eventGuest.eventId,
                                eventGuest.name,
                                eventGuest.description,
                                eventGuest.imageKey,
                                eventGuest.status,
                                eventGuest.visibility,
                                eventGuest.createdAt
                        ),
                        relevance == null ? Expressions.nullExpression(Double.class) : relevance
                ))
                .from(eventGuest)
                .where(where);
        if (relevance != null) {
            query.orderBy(relevance.desc(), eventGuest.createdAt.desc(), eventGuest.id.desc());
        } else {
            query.orderBy(eventGuest.createdAt.desc(), eventGuest.id.desc());
        }
        return query.limit(limit).fetch();
    }
}

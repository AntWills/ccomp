package com.ccomp.br.domain.events.guests.persistence.dsl;

import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.guests.persistence.QEventGuest.eventGuest;

@Repository
public class EventGuestDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventGuestDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventGuest> findGuestsWithCursor(
            Long eventId, boolean isManager, GuestCursor cursor, int limit) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(eventGuest.event.id.eq(eventId));

        // Se não for gestor, filtra apenas os públicos e confirmados
        if (!isManager) {
            where.and(eventGuest.visibility.eq(EnumGuestVisibility.PUBLIC))
                    .and(eventGuest.status.eq(EnumGuestStatus.CONFIRMED));
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    eventGuest.createdAt, eventGuest.id,
                    cursor.createdAt(), cursor.id()
            );
            where.and(cursorCondition);
        }

        return queryFactory
                .selectFrom(eventGuest)
                .where(where)
                .orderBy(eventGuest.createdAt.desc(), eventGuest.id.desc())
                .limit(limit)
                .fetch();
    }
}

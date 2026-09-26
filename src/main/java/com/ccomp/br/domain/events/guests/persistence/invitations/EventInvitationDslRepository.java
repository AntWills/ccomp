package com.ccomp.br.domain.events.guests.persistence.invitations;

import com.ccomp.br.domain.events.guests.dto.EventInvitationCursor;
import com.ccomp.br.module.email.EmailAddress;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.ccomp.br.domain.events.guests.persistence.invitations.QEventInvitation.eventInvitation;

@Repository
public class EventInvitationDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventInvitationDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventInvitation> findAllWithCursor(
            @Nullable EmailAddress emailAddress, Long eventId, EventInvitationCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        Optional.ofNullable(emailAddress).ifPresent(em ->
                whereClause.and(eventInvitation.emailAddress.eq(em)));

        whereClause.and(eventInvitation.event.id.eq(eventId));

        if(cursor != null && cursor.id() != null && cursor.invitedAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    eventInvitation.invitedAt,
                    eventInvitation.id,
                    cursor.invitedAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .selectFrom(eventInvitation)
                .where(whereClause)
                .orderBy(
                        eventInvitation.invitedAt.desc(),
                        eventInvitation.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}

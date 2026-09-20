package com.ccomp.br.domain.events.guests.persistence.dsl;

import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.guests.persistence.QActivityGuest.activityGuest;

@Repository
public class ActivityGuestDslRepository {
    private final JPAQueryFactory queryFactory;

    public ActivityGuestDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ActivityGuest> findActivityGuestsWithCursor(
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
                .selectFrom(activityGuest)
                .innerJoin(activityGuest.eventGuest).fetchJoin() // Traz o EventGuest junto para evitar N+1
                .where(where)
                .orderBy(activityGuest.createdAt.desc(), activityGuest.id.desc())
                .limit(limit)
                .fetch();
    }
}

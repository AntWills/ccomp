package com.ccomp.br.domain.events.materials.persistence;

import com.ccomp.br.domain.events.materials.dto.EventMaterialCursor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.materials.persistence.QEventMaterial.eventMaterial;

@Repository
public class EventMaterialDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventMaterialDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventMaterial> findAllByEventId(Long eventId) {
        return queryFactory.selectFrom(eventMaterial)
                .where(eventMaterial.eventId.eq(eventId))
                .orderBy(eventMaterial.createdAt.desc(), eventMaterial.id.desc())
                .fetch();
    }

    public List<EventMaterial> findAllByEventIdWithCursor(
            Long eventId, @Nullable EventMaterialCursor cursor, int limit) {
        BooleanExpression eventCondition = eventMaterial.eventId.eq(eventId);
        BooleanExpression whereCondition = eventCondition;

        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            BooleanExpression afterCursor = eventMaterial.createdAt.lt(cursor.createdAt())
                    .or(eventMaterial.createdAt.eq(cursor.createdAt())
                            .and(eventMaterial.id.lt(cursor.id())));
            whereCondition = eventCondition.and(afterCursor);
        }

        return queryFactory.selectFrom(eventMaterial)
                .where(whereCondition)
                .orderBy(eventMaterial.createdAt.desc(), eventMaterial.id.desc())
                .limit(limit)
                .fetch();
    }
}

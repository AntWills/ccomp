package com.ccomp.br.domain.events.activities.materials.persistence;

import com.ccomp.br.domain.events.materials.dto.MaterialCursor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.activities.materials.persistence.QActivityMaterial.activityMaterial;

@Repository
public class ActivityMaterialDslRepository {
    private final JPAQueryFactory queryFactory;

    public ActivityMaterialDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ActivityMaterial> findAllByActivityId(Long activityId) {
        return queryFactory.selectFrom(activityMaterial)
                .where(activityMaterial.activityId.eq(activityId))
                .orderBy(activityMaterial.createdAt.desc(), activityMaterial.id.desc())
                .fetch();
    }

    public List<ActivityMaterial> findAllByActivityIdWithCursor(
            Long activityId, @Nullable MaterialCursor cursor, int limit) {
        BooleanExpression activityCondition = activityMaterial.activityId.eq(activityId);
        BooleanExpression whereCondition = activityCondition;

        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            BooleanExpression afterCursor = activityMaterial.createdAt.lt(cursor.createdAt())
                    .or(activityMaterial.createdAt.eq(cursor.createdAt())
                            .and(activityMaterial.id.lt(cursor.id())));
            whereCondition = activityCondition.and(afterCursor);
        }

        return queryFactory.selectFrom(activityMaterial)
                .where(whereCondition)
                .orderBy(activityMaterial.createdAt.desc(), activityMaterial.id.desc())
                .limit(limit)
                .fetch();
    }
}

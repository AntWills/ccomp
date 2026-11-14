package com.ccomp.br.domain.clubs.materials.persistence;

import com.ccomp.br.domain.clubs.materials.dto.ClubMaterialCursor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.clubs.materials.persistence.QClubMaterial.clubMaterial;

@Repository
public class ClubMaterialDslRepository {
    private final JPAQueryFactory queryFactory;

    public ClubMaterialDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ClubMaterial> findAllByClubId(Long clubId) {
        return queryFactory.selectFrom(clubMaterial)
                .where(clubMaterial.clubId.eq(clubId))
                .orderBy(clubMaterial.createdAt.desc(), clubMaterial.id.desc())
                .fetch();
    }

    public List<ClubMaterial> findAllByClubIdWithCursor(
            Long clubId, @Nullable ClubMaterialCursor cursor, int limit) {
        BooleanExpression clubCondition = clubMaterial.clubId.eq(clubId);
        BooleanExpression whereCondition = clubCondition;

        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            BooleanExpression afterCursor = clubMaterial.createdAt.lt(cursor.createdAt())
                    .or(clubMaterial.createdAt.eq(cursor.createdAt())
                            .and(clubMaterial.id.lt(cursor.id())));
            whereCondition = clubCondition.and(afterCursor);
        }

        return queryFactory.selectFrom(clubMaterial)
                .where(whereCondition)
                .orderBy(clubMaterial.createdAt.desc(), clubMaterial.id.desc())
                .limit(limit)
                .fetch();
    }
}

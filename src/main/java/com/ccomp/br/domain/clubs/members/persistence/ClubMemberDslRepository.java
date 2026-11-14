package com.ccomp.br.domain.clubs.members.persistence;

import com.ccomp.br.domain.clubs.members.dto.ClubMemberCursor;
import com.ccomp.br.domain.clubs.members.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.members.dto.ClubMemberListItem;
import com.ccomp.br.shared.dto.UserSummaryDTO;
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

import static com.ccomp.br.domain.clubs.members.persistence.QClubMember.clubMember;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class ClubMemberDslRepository {
    private static final double MIN_SIMILARITY = 0.4;

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    public ClubMemberDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ClubMemberListItem> findAllWithCursor(
            Long clubId, ClubMemberFilter filter, @Nullable ClubMemberCursor cursor, int limit) {
        return findAllWithCursor(clubId, filter, null, cursor, limit);
    }

    public List<ClubMemberListItem> findAllWithCursor(
            Long clubId, ClubMemberFilter filter, @Nullable String searchTerm,
            @Nullable ClubMemberCursor cursor, int limit) {

        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(clubMember.club.id.eq(clubId));

        filter.roleOpt().ifPresent(r ->
                whereClause.and(clubMember.role.eq(filter.role()))
        );

        filter.statusOpt().ifPresent(s ->
                whereClause.and(clubMember.status.eq(filter.status()))
        );

        if (searchTerm != null) {
            useTextSearchThreshold();
            whereClause.and(TextSearchDSL.matchAny(searchTerm, MIN_SIMILARITY,
                    userModel.name, userModel.emailAddress.value));
        }

        if (cursor != null && cursor.id() != null && cursor.joinedAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    clubMember.joinedAt,
                    clubMember.id,
                    cursor.joinedAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                        Projections.constructor(
                                ClubMemberListItem.class,
                                Projections.constructor(
                                        UserSummaryDTO.class,
                                        userModel.id,
                                        userModel.name,
                                        userModel.emailAddress
                                ),
                                clubMember.id,
                                clubMember.club.id,
                                clubMember.role,
                                clubMember.status,
                                clubMember.joinedAt,
                                clubMember.leftAt
                        )
                )
                .from(clubMember)
                .innerJoin(userModel).on(userModel.id.eq(clubMember.userId))
                .where(whereClause)
                .orderBy(
                        clubMember.joinedAt.desc(),
                        clubMember.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    private void useTextSearchThreshold() {
        entityManager.createNativeQuery(
                        "select set_config('pg_trgm.word_similarity_threshold', '0.399999', true)")
                .getSingleResult();
    }
}

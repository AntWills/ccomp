package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.domain.clubs.dto.ClubMemberCursor;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.clubs.persistence.members.QClubMember.clubMember;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class ClubMemberDslRepository {

    private final JPAQueryFactory queryFactory;

    public ClubMemberDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<ClubMemberListItem> findAllWithCursor(
            Long clubId, ClubMemberFilter filter, @Nullable ClubMemberCursor cursor, int limit) {

        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(clubMember.club.id.eq(clubId));

        filter.roleOpt().ifPresent(r ->
                whereClause.and(clubMember.role.eq(filter.role()))
        );

        filter.statusOpt().ifPresent(s ->
                whereClause.and(clubMember.status.eq(filter.status()))
        );

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
}
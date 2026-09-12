package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.dto.UserCursor;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.shared.dto.UserItemDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;
import static com.ccomp.br.domain.users.persistence.roles.QRoles.roles;

@Repository
public class UserDslRepository {
    private final JPAQueryFactory queryFactory;

    public UserDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<UserItemDTO> findByCursor(UserSearchFilter filter, @Nullable UserCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        filter.statusAccountOpt().ifPresent(sa ->
            whereClause.and(userModel.statusAccount.eq(sa))
        );

        filter.roleOpt().ifPresent(role ->
                whereClause.and(userModel.role.role.eq(role))
        );

        if(cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    userModel.createdAt,
                    userModel.id,
                    cursor.createdAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                        Projections.constructor(
                                UserItemDTO.class,
                                userModel.id,
                                userModel.name,
                                userModel.emailAddress,
                                userModel.statusAccount,
                                userModel.createdAt,
                                userModel.updatedAt,
                                roles.role
                        )
                )
                .from(userModel)
                .innerJoin(userModel.role, roles)
                .where(whereClause)
                .orderBy(
                        userModel.createdAt.desc(),
                        userModel.id.desc())
                .limit(limit)
                .fetch();
    }

}

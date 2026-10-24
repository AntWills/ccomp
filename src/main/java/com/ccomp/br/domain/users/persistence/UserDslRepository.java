package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.dto.UserCursor;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.dto.UserSearchResult;
import com.ccomp.br.shared.dto.UserItemDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
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

    public List<UserSearchResult> findByCursor(UserSearchFilter filter, @Nullable UserCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();
        var searchTerm = filter.searchTermOpt();

        var termExpr = searchTerm.map(Expressions::constant).orElseGet(() -> Expressions.constant(""));

        // Força o tipo Double via cast() no HQL para o TypecheckUtil do Hibernate 7 aceitar a operação de adição (+)
        NumberExpression<Double> nameWordSim = Expressions.numberTemplate(
                Double.class, "cast(function('word_similarity', {0}, {1}) as double)", termExpr, userModel.name);
        NumberExpression<Double> nameSim = Expressions.numberTemplate(
                Double.class, "cast(function('similarity', {0}, {1}) as double)", termExpr, userModel.name);

        NumberExpression<Double> emailWordSim = Expressions.numberTemplate(
                Double.class, "cast(function('word_similarity', {0}, {1}) as double)", termExpr, userModel.emailAddress.value);
        NumberExpression<Double> emailSim = Expressions.numberTemplate(
                Double.class, "cast(function('similarity', {0}, {1}) as double)", termExpr, userModel.emailAddress.value);

        // Soma os scores de substring (word_similarity) e exatidão (similarity)
        NumberExpression<Double> nameScore = nameWordSim.add(nameSim);
        NumberExpression<Double> emailScore = emailWordSim.add(emailSim);

        // Maior pontuação entre nome e e-mail
        NumberExpression<Double> similarity = Expressions.numberTemplate(
                Double.class,
                "greatest({0}, {1})",
                nameScore,
                emailScore
        );

        filter.statusAccountOpt().ifPresent(sa ->
                whereClause.and(userModel.statusAccount.eq(sa))
        );

        filter.roleOpt().ifPresent(role ->
                whereClause.and(userModel.role.role.eq(role))
        );

        searchTerm.ifPresent(term -> {
            var tExpr = Expressions.constant(term);

            var nameWordSimWhere = Expressions.numberTemplate(
                    Double.class,
                    "cast(function('word_similarity', {0}, {1}) as double)",
                    tExpr,
                    userModel.name
            );

            var emailWordSimWhere = Expressions.numberTemplate(
                    Double.class,
                    "cast(function('word_similarity', {0}, {1}) as double)",
                    tExpr,
                    userModel.emailAddress.value
            );

            whereClause.and(nameWordSimWhere.goe(0.4).or(emailWordSimWhere.goe(0.4)));
        });

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null
                && searchTerm.isPresent() && cursor.similarity() != null) {
            whereClause.and(Expressions.booleanTemplate(
                    "({0}, {1}, {2}) < ({3}, {4}, {5})",
                    similarity,
                    userModel.createdAt,
                    userModel.id,
                    Expressions.constant(cursor.similarity()),
                    Expressions.constant(cursor.createdAt()),
                    Expressions.constant(cursor.id())
            ));
        } else if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    userModel.createdAt,
                    userModel.id,
                    Expressions.constant(cursor.createdAt()),
                    Expressions.constant(cursor.id())
            );
            whereClause.and(cursorCondition);
        }

        var query = queryFactory.select(
                        Projections.constructor(
                                UserSearchResult.class,
                                Projections.constructor(
                                        UserItemDTO.class,
                                        userModel.id,
                                        userModel.name,
                                        userModel.emailAddress,
                                        userModel.statusAccount,
                                        userModel.createdAt,
                                        userModel.updatedAt,
                                        roles.role
                                ),
                                similarity
                        )
                )
                .from(userModel)
                .innerJoin(userModel.role, roles)
                .where(whereClause);

        if (searchTerm.isPresent()) {
            query.orderBy(similarity.desc(), userModel.createdAt.desc(), userModel.id.desc());
        } else {
            query.orderBy(userModel.createdAt.desc(), userModel.id.desc());
        }

        return query
                .limit(limit)
                .fetch();
    }
}
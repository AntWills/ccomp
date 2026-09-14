package com.ccomp.br.domain.events.enrollments.persistence;


import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EnrollmentDslRepository {
    private final JPAQueryFactory queryFactory;

    public EnrollmentDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EnrollmentListItem> findAllWithCursor(Long eventId, @Nullable EnrollmentsCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(enrollment.event.id.eq(eventId));

        if(cursor != null && cursor.id()!= null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    enrollment.createdAt,
                    enrollment.id,
                    cursor.createdAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                        Projections.constructor(
                                EnrollmentListItem.class,
                                Projections.constructor(
                                        UserSummaryDTO.class,
                                        userModel.id,
                                        userModel.name,
                                        userModel.emailAddress
                                ),
                                enrollment.id,
                                enrollment.status,
                                enrollment.createdAt
                        )
                )
                .from(enrollment)
                .innerJoin(userModel).on(userModel.id.eq(enrollment.userId))
                .where(whereClause)
                .orderBy(
                        enrollment.createdAt.desc(),
                        enrollment.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}

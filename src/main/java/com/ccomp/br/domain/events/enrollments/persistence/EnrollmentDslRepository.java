package com.ccomp.br.domain.events.enrollments.persistence;


import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentSearchResult;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.shared.utils.TextSearchDSL;
import com.ccomp.br.shared.utils.TextSearchSession;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EnrollmentDslRepository {
    private final JPAQueryFactory queryFactory;
    private final TextSearchSession textSearch;

    public EnrollmentDslRepository(JPAQueryFactory queryFactory, TextSearchSession textSearch) {
        this.queryFactory = queryFactory;
        this.textSearch = textSearch;
    }

    public List<EnrollmentListItem> findAllWithCursor(Long eventId, @Nullable EnrollmentsCursor cursor, int limit) {
        return findAllWithCursor(eventId, null, cursor, limit);
    }

    public List<EnrollmentListItem> findAllWithCursor(Long eventId, @Nullable EnumEnrollmentState status,
                                                      @Nullable EnrollmentsCursor cursor, int limit) {
        return searchWithCursor(eventId, status, null, cursor, limit).stream()
                .map(EnrollmentSearchResult::enrollment)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentSearchResult> searchWithCursor(Long eventId, @Nullable EnumEnrollmentState status,
                                                          @Nullable String searchTerm,
                                                          @Nullable EnrollmentsCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();
        String normalizedTerm = searchTerm == null ? null : searchTerm.trim();
        if (normalizedTerm != null && normalizedTerm.isBlank()) normalizedTerm = null;

        whereClause.and(enrollment.event.id.eq(eventId));
        if (status != null) {
            whereClause.and(enrollment.status.eq(status));
        }

        var relevance = normalizedTerm == null ? null : TextSearchDSL.bestScore(normalizedTerm,
                userModel.name, userModel.emailAddress.value);
        if (normalizedTerm != null) {
            textSearch.useWordSimilarityThreshold(0.4);
            whereClause.and(TextSearchDSL.matchAny(normalizedTerm, 0.4,
                    userModel.name, userModel.emailAddress.value));
        }

        if(cursor != null && cursor.id()!= null && cursor.createdAt() != null) {
            if (relevance != null && cursor.relevance() != null) {
                whereClause.and(Expressions.booleanTemplate(
                        "({0}, {1}, {2}) < ({3}, {4}, {5})",
                        relevance, enrollment.createdAt, enrollment.id,
                        Expressions.constant(cursor.relevance()),
                        Expressions.constant(cursor.createdAt()),
                        Expressions.constant(cursor.id())
                ));
            } else {
                BooleanExpression cursorCondition = Expressions.booleanTemplate(
                        "( {0}, {1} ) < ( {2}, {3} )",
                        enrollment.createdAt, enrollment.id,
                        Expressions.constant(cursor.createdAt()),
                        Expressions.constant(cursor.id())
                );
                whereClause.and(cursorCondition);
            }
        }

        var query = queryFactory.select(Projections.constructor(
                        EnrollmentSearchResult.class,
                        Projections.constructor(EnrollmentListItem.class,
                                Projections.constructor(UserSummaryDTO.class, userModel.id,
                                        userModel.name, userModel.emailAddress),
                                enrollment.id, enrollment.status, enrollment.createdAt),
                        relevance == null ? Expressions.nullExpression(Double.class) : relevance))
                .from(enrollment)
                .innerJoin(userModel).on(userModel.id.eq(enrollment.userId))
                .where(whereClause);
        if (relevance != null) {
            query.orderBy(relevance.desc(), enrollment.createdAt.desc(), enrollment.id.desc());
        } else {
            query.orderBy(enrollment.createdAt.desc(), enrollment.id.desc());
        }
        return query.limit(limit).fetch();
    }
}

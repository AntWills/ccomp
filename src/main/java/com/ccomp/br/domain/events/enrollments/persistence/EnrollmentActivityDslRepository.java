package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.domain.events.enrollments.dto.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentActivityCursor;
import com.ccomp.br.domain.events.enrollments.dto.ActivityEnrollmentSearchResult;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.shared.utils.TextSearchDSL;
import com.ccomp.br.shared.utils.TextSearchSession;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollmentActivity.enrollmentActivity;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Component
public class EnrollmentActivityDslRepository {
    private final JPAQueryFactory queryFactory;
    private final TextSearchSession textSearch;

    public EnrollmentActivityDslRepository(JPAQueryFactory queryFactory, TextSearchSession textSearch) {
        this.queryFactory = queryFactory;
        this.textSearch = textSearch;
    }

    @Transactional(readOnly = true)
    public Optional<EnrollmentActivity> findByUserIdAndActivityId(UUID userId, Long activityId) {
        var result = queryFactory
                .selectFrom(enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment).fetchJoin()
                .where(
                        enrollment.userId.eq(userId),
                        enrollment.status.in(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN),
                        enrollmentActivity.activity.id.eq(activityId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Transactional(readOnly = true)
    public long countActiveByActivityId(Long activityId) {
        Long count = queryFactory
                .select(enrollmentActivity.id.count())
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(enrollmentActivity.activity.id.eq(activityId),
                        enrollment.status.in(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN))
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    public List<ActivityEnrollmentSearchResult> findAllUsersByActivityId(Long activityId, String searchTerm,
                                                                          EnrollmentActivityCursor cursor,
                                                                          int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();
        String normalizedTerm = searchTerm == null ? null : searchTerm.trim();
        if (normalizedTerm != null && normalizedTerm.isBlank()) normalizedTerm = null;
        var relevance = normalizedTerm == null ? null : TextSearchDSL.bestScore(normalizedTerm,
                userModel.name, userModel.emailAddress.value);
        if (normalizedTerm != null) {
            textSearch.useWordSimilarityThreshold(0.4);
            whereClause.and(TextSearchDSL.matchAny(normalizedTerm, 0.4,
                    userModel.name, userModel.emailAddress.value));
        }

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            if (relevance != null && cursor.relevance() != null) {
                whereClause.and(Expressions.booleanTemplate(
                        "({0}, {1}, {2}) < ({3}, {4}, {5})",
                        relevance, enrollmentActivity.createdAt, enrollmentActivity.id,
                        Expressions.constant(cursor.relevance()), Expressions.constant(cursor.createdAt()),
                        Expressions.constant(cursor.id())));
            } else {
                BooleanExpression cursorCondition = Expressions.booleanTemplate(
                        "({0}, {1}) < ({2}, {3})", enrollmentActivity.createdAt, enrollmentActivity.id,
                        Expressions.constant(cursor.createdAt()), Expressions.constant(cursor.id()));
                whereClause.and(cursorCondition);
            }
        }

        var query = queryFactory
                .select(Projections.constructor(
                        ActivityEnrollmentSearchResult.class,
                        Projections.constructor(
                                UserActivitySummaryDTO.class,
                                Projections.constructor(UserSummaryDTO.class, userModel.id, userModel.name,
                                        userModel.emailAddress),
                                enrollmentActivity.attendedAt.isNotNull(), enrollmentActivity.id,
                                enrollmentActivity.createdAt),
                        relevance == null ? Expressions.nullExpression(Double.class) : relevance
                ))
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .innerJoin(userModel).on(userModel.id.eq(enrollment.userId))
                .where(
                        enrollmentActivity.activity.id.eq(activityId),
                        whereClause
                )
                ;
        if (relevance == null) {
            query.orderBy(enrollmentActivity.createdAt.desc(), enrollmentActivity.id.desc());
        } else {
            query.orderBy(relevance.desc(), enrollmentActivity.createdAt.desc(), enrollmentActivity.id.desc());
        }
        return query.limit(limit).fetch();
    }
}

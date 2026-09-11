package com.ccomp.br.domain.events.persistence.activities;

import com.ccomp.br.domain.events.dto.enrollments.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enums.activities.EnrollmentActivityCursor;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ccomp.br.domain.events.persistence.activities.QEnrollmentActivity.enrollmentActivity;
import static com.ccomp.br.domain.events.persistence.enrollments.QEnrollment.enrollment;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Component
public class EnrollmentActivityDslRepository {
    private final JPAQueryFactory queryFactory;

    public EnrollmentActivityDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<EnrollmentActivity> findByUserIdAndActivityId(UUID userId, Long activityId) {
        var result = queryFactory
                .selectFrom(enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .where(
                        enrollment.userId.eq(userId),
                        enrollmentActivity.activity.id.eq(activityId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public List<UserActivitySummaryDTO> findAllUsersByActivityId(Long activityId,
                                                                 EnrollmentActivityCursor cursor,
                                                                 int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        if (cursor != null && cursor.id() != null) {
            whereClause.and(
                    enrollmentActivity.createdAt.lt(cursor.createdAt())
                            .or(
                                    enrollmentActivity.createdAt.eq(cursor.createdAt())
                                            .and(enrollmentActivity.id.lt(cursor.id()))
                            )
            );
        }

        return queryFactory
                .select(Projections.constructor(
                        UserActivitySummaryDTO.class,
                        Projections.constructor(
                                UserSummaryDTO.class,
                                userModel.id,
                                userModel.name,
                                userModel.emailAddress
                        ),
                        enrollmentActivity.id,
                        enrollmentActivity.createdAt
                ))
                .from(enrollmentActivity)
                .innerJoin(enrollmentActivity.enrollment, enrollment)
                .innerJoin(userModel).on(userModel.id.eq(enrollment.userId))
                .where(
                        enrollmentActivity.activity.id.eq(activityId),
                        whereClause
                )
                .orderBy(
                        enrollmentActivity.createdAt.desc(),
                        enrollmentActivity.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}

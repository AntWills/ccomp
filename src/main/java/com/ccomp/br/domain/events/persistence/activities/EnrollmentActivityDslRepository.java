package com.ccomp.br.domain.events.persistence.activities;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.ccomp.br.domain.events.persistence.activities.QEnrollmentActivity.enrollmentActivity;
import static com.ccomp.br.domain.events.persistence.enrollments.QEnrollment.enrollment;

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
}

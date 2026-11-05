package com.ccomp.br.domain.events.credential.persistence;

import com.ccomp.br.domain.events.credential.dto.CredentialSearchItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.credential.persistence.QEventCredential.eventCredential;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EventCredentialDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventCredentialDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<CredentialSearchItem> searchForCredentialing(Long eventId, String searchTerm, int limit) {
        BooleanExpression matchesParticipant = userModel.name.containsIgnoreCase(searchTerm)
                .or(userModel.emailAddress.value.containsIgnoreCase(searchTerm));
        BooleanExpression eligible = enrollment.status.in(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN);

        return queryFactory.select(Projections.constructor(
                        CredentialSearchItem.class,
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
                        ),
                        eligible,
                        eventCredential.credentialedAt.isNotNull()
                ))
                .from(enrollment)
                .innerJoin(userModel).on(userModel.id.eq(enrollment.userId))
                .leftJoin(eventCredential).on(eventCredential.eventId.eq(eventId)
                        .and(eventCredential.enrollmentId.eq(enrollment.id)))
                .where(enrollment.event.id.eq(eventId).and(matchesParticipant))
                .orderBy(enrollment.createdAt.desc(), enrollment.id.desc())
                .limit(limit)
                .fetch();
    }
}

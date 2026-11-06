package com.ccomp.br.domain.events.credential.persistence;

import com.ccomp.br.domain.events.credential.dto.CredentialSearchItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.shared.utils.TextSearchDSL;
import com.ccomp.br.shared.utils.TextSearchSession;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ccomp.br.domain.events.credential.persistence.QEventCredential.eventCredential;
import static com.ccomp.br.domain.events.enrollments.persistence.QEnrollment.enrollment;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EventCredentialDslRepository {
    private static final double MIN_SIMILARITY = 0.4;

    private final JPAQueryFactory queryFactory;
    private final TextSearchSession textSearch;
    private final EntityManager entityManager;

    public EventCredentialDslRepository(JPAQueryFactory queryFactory, TextSearchSession textSearch, EntityManager entityManager) {
        this.queryFactory = queryFactory;
        this.textSearch = textSearch;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<CredentialSearchItem> searchForCredentialing(Long eventId, String searchTerm, int limit) {
        // GIN indexes from V22 support `<%`. Keep the candidate threshold slightly lower
        // because the operator is strict, then preserve the documented >= 0.4 boundary.
//        entityManager.createNativeQuery(
//                "select set_config('pg_trgm.word_similarity_threshold', '0.399999', true)")
//                .getSingleResult();

        textSearch.useWordSimilarityThreshold(MIN_SIMILARITY);

        var matches = TextSearchDSL.matchAny(searchTerm, MIN_SIMILARITY,
                userModel.name, userModel.emailAddress.value);
        var relevance = TextSearchDSL.bestScore(searchTerm,
                userModel.name, userModel.emailAddress.value);

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
                .where(enrollment.event.id.eq(eventId).and(matches))
                .orderBy(relevance.desc(), enrollment.createdAt.desc(), enrollment.id.desc())
                .limit(limit)
                .fetch();
    }
}

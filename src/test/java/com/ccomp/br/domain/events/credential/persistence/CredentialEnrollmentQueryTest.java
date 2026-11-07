package com.ccomp.br.domain.events.credential.persistence;

import com.ccomp.br.domain.auth.passwordreset.application.TokenHasher;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.credential.persistence.EventCredentialDslRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.utils.TextSearchSession;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, EventCredentialDslRepository.class, TextSearchSession.class})
class CredentialEnrollmentQueryTest {
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserModelRepository userRepository;
    @Autowired private EventCredentialRepository credentialRepository;
    @Autowired private EventCredentialDslRepository credentialDslRepository;
    @Autowired private EntityManager entityManager;

    private Long eventId;

    @BeforeEach
    void setUp() {
        Event event = eventRepository.saveAndFlush(Event.builder()
                .title("Credenciamento SQL")
                .slug("credenciamento-sql-" + UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .status(EnumEventStatus.PUBLISHED)
                .build());
        eventId = event.getId();

        saveEnrollment(event, "Ana Participante", "ana.participante@example.org");
        saveEnrollment(event, "Bruno Participante", "bruno@example.org");

        Event anotherEvent = eventRepository.saveAndFlush(Event.builder()
                .title("Outro evento")
                .slug("outro-evento-" + UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .status(EnumEventStatus.PUBLISHED)
                .build());
        saveEnrollment(anotherEvent, "Ana OutroEvento", "ana.outro@example.org");
    }

    @Test
    void searchMatchesNameAndEmailAndRestrictsResultsToEvent() {
        var nameResults = credentialDslRepository.searchForCredentialing(eventId, "ana partic", 20);
        var emailResults = credentialDslRepository.searchForCredentialing(eventId, "bruno@example", 20);
        var otherEventResults = credentialDslRepository.searchForCredentialing(eventId, "outroevento", 20);

        assertThat(nameResults).extracting(result -> result.enrollment().user().name())
                .contains("Ana Participante");
        assertThat(nameResults).anySatisfy(result -> {
            assertThat(result.eligible()).isTrue();
            assertThat(result.credentialed()).isFalse();
        });
        assertThat(emailResults).extracting(result -> result.enrollment().user().emailAddress().getValue())
                .contains("bruno@example.org");
        assertThat(otherEventResults).isEmpty();
        assertThat(entityManager.createNativeQuery(
                "select current_setting('pg_trgm.word_similarity_threshold')").getSingleResult())
                .isEqualTo("0.399999");
        Number trigramIndexes = (Number) entityManager.createNativeQuery("""
                select count(*) from pg_indexes
                where schemaname = current_schema()
                  and indexname in ('idx_users_name_trgm', 'idx_users_email_address_trgm')
                """).getSingleResult();
        assertThat(trigramIndexes.intValue()).isEqualTo(2);
    }

    @Test
    void genericTextSearchMatchCanUseExistingGinIndex() {
        entityManager.createNativeQuery(
                "select set_config('pg_trgm.word_similarity_threshold', '0.399999', true)").getSingleResult();
        entityManager.createNativeQuery("select set_config('enable_seqscan', 'off', true)").getSingleResult();

        @SuppressWarnings("unchecked")
        var plan = (java.util.List<String>) entityManager.createNativeQuery("""
                explain select id from tb_users
                where text_search_match('ana partic', name, 0.4)
                """).getResultList();

        assertThat(String.join("\n", plan)).contains("idx_users_name_trgm");

        Number score = (Number) entityManager.createNativeQuery(
                "select text_search_score('ana partic', 'Ana Participante')").getSingleResult();
        assertThat(score.doubleValue()).isGreaterThan(0.4);
    }

    @Test
    void tokenLookupReturnsEnrollmentOnlyForMatchingEventAndCredentialCount() {
        var enrollment = credentialDslRepository.searchForCredentialing(eventId, "ana.participante", 1)
                .getFirst().enrollment();
        String rawToken = "opaque-token-for-test";
        LocalDateTime now = LocalDateTime.now();
        credentialRepository.saveAndFlush(EventCredential.builder()
                .eventId(eventId)
                .enrollmentId(enrollment.id())
                .credentialedAt(now)
                .credentialedBy(enrollment.user().id())
                .station("A")
                .tokenHash(TokenHasher.hash(rawToken))
                .tokenUpdatedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());

        assertThat(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId, TokenHasher.hash(rawToken)))
                .contains(enrollment.id());
        assertThat(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId + 1, TokenHasher.hash(rawToken)))
                .isEmpty();
        assertThat(credentialRepository.countByEventIdAndCredentialedAtIsNotNull(eventId)).isEqualTo(1);
        var searchResult = credentialDslRepository.searchForCredentialing(eventId, "ana.participante", 1).getFirst();
        assertThat(searchResult.credentialed()).isTrue();
    }

    private void saveEnrollment(Event event, String name, String email) {
        LocalDateTime now = LocalDateTime.now();
        UserModel user = userRepository.saveAndFlush(UserModel.builder()
                .name(name)
                .emailAddress(new EmailAddress(email))
                .password("test-hash")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
        enrollmentRepository.saveAndFlush(Enrollment.builder()
                .event(event)
                .userId(user.getId())
                .status(EnumEnrollmentState.CONFIRMED)
                .build());
    }
}

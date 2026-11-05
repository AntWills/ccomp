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
@Import({QueryDslConfig.class, EventCredentialDslRepository.class})
class CredentialEnrollmentQueryTest {
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserModelRepository userRepository;
    @Autowired private EventCredentialRepository credentialRepository;
    @Autowired private EventCredentialDslRepository credentialDslRepository;

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

        assertThat(nameResults).hasSize(1);
        assertThat(nameResults.getFirst().enrollment().user().name()).isEqualTo("Ana Participante");
        assertThat(nameResults.getFirst().eligible()).isTrue();
        assertThat(nameResults.getFirst().credentialed()).isFalse();
        assertThat(emailResults).hasSize(1);
        assertThat(emailResults.getFirst().enrollment().user().emailAddress().getValue())
                .isEqualTo("bruno@example.org");
        assertThat(otherEventResults).isEmpty();
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

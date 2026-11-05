package com.ccomp.br.domain.events.credential.persistence;

import com.ccomp.br.domain.auth.passwordreset.application.TokenHasher;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CredentialEnrollmentQueryTest {
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserModelRepository userRepository;
    @Autowired private EventCredentialRepository credentialRepository;

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
        var nameResults = enrollmentRepository.searchForCredentialing(eventId, "ana partic", org.springframework.data.domain.PageRequest.of(0, 20));
        var emailResults = enrollmentRepository.searchForCredentialing(eventId, "bruno@example", org.springframework.data.domain.PageRequest.of(0, 20));
        var otherEventResults = enrollmentRepository.searchForCredentialing(eventId, "outroevento", org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(nameResults).hasSize(1);
        assertThat(nameResults.getFirst().getUserId()).isNotNull();
        assertThat(emailResults).hasSize(1);
        assertThat(otherEventResults).isEmpty();
    }

    @Test
    void tokenLookupReturnsEnrollmentOnlyForMatchingEventAndCredentialCount() {
        var enrollment = enrollmentRepository.searchForCredentialing(eventId, "ana.participante", 
                org.springframework.data.domain.PageRequest.of(0, 1)).getFirst();
        String rawToken = "opaque-token-for-test";
        LocalDateTime now = LocalDateTime.now();
        credentialRepository.saveAndFlush(EventCredential.builder()
                .eventId(eventId)
                .enrollmentId(enrollment.getId())
                .credentialedAt(now)
                .credentialedBy(enrollment.getUserId())
                .station("A")
                .tokenHash(TokenHasher.hash(rawToken))
                .tokenUpdatedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());

        assertThat(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId, TokenHasher.hash(rawToken)))
                .contains(enrollment.getId());
        assertThat(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId + 1, TokenHasher.hash(rawToken)))
                .isEmpty();
        assertThat(credentialRepository.countByEventIdAndCredentialedAtIsNotNull(eventId)).isEqualTo(1);
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

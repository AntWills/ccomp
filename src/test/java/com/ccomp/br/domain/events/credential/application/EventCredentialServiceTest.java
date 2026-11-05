package com.ccomp.br.domain.events.credential.application;

import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.auth.passwordreset.application.TokenHasher;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.credential.dto.CredentialScanRequest;
import com.ccomp.br.domain.events.credential.dto.CredentialResponse;
import com.ccomp.br.domain.events.credential.dto.CredentialSearchRequest;
import com.ccomp.br.domain.events.credential.dto.CredentialSummaryResponse;
import com.ccomp.br.domain.events.credential.dto.CredentialTokenResponse;
import com.ccomp.br.domain.events.credential.persistence.EventCredential;
import com.ccomp.br.domain.events.credential.persistence.EventCredentialOperatorRepository;
import com.ccomp.br.domain.events.credential.persistence.EventCredentialRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCredentialServiceTest {
    @Mock private EventRepository eventRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EventCredentialRepository credentialRepository;
    @Mock private EventCredentialOperatorRepository operatorRepository;
    @Mock private UserModelRepository userRepository;
    @Mock private EventAccessPolicy accessPolicy;
    @Mock private AuditLogRepository auditRepository;

    @InjectMocks private EventCredentialService service;

    private final Long eventId = 41L;
    private final Long enrollmentId = 81L;
    private final UUID actor = UUID.randomUUID();
    private Event event;
    private Enrollment enrollment;
    private UserModel user;

    @BeforeEach
    void setUp() {
        event = mock(Event.class);
        enrollment = Enrollment.builder().id(enrollmentId).userId(actor).status(EnumEnrollmentState.CONFIRMED).build();
        ReflectionTestUtils.setField(enrollment, "eventId", eventId);
        user = UserModel.builder().id(actor).name("Ana Souza")
                .emailAddress(new EmailAddress("ana.souza@example.org")).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        lenient().when(enrollmentRepository.findByIdAndEventId(enrollmentId, eventId)).thenReturn(Optional.of(enrollment));
        lenient().when(operatorRepository.existsByEventIdAndUserId(eventId, actor)).thenReturn(false);
        lenient().when(credentialRepository.save(any(EventCredential.class))).thenAnswer(invocation -> {
            EventCredential saved = invocation.getArgument(0);
            if (saved.getId() == null) ReflectionTestUtils.setField(saved, "id", 900L);
            return saved;
        });
        lenient().when(userRepository.findById(actor)).thenReturn(Optional.of(user));
    }

    @Test
    void scan_validTokenRegistersArrivalAndAudit() {
        String rawToken = "opaque-token";
        EventCredential credential = credential(null);
        when(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId, TokenHasher.hash(rawToken)))
                .thenReturn(Optional.of(enrollmentId));
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.of(credential));

        CredentialResponse response = service.scan(eventId, actor, new CredentialScanRequest(rawToken, "A"));

        assertThat(response.result()).isEqualTo("CREDENTIALED");
        assertThat(response.enrollmentStatus()).isEqualTo("CHECKED_IN");
        assertThat(response.emailMasked()).isEqualTo("a***@example.org");
        assertThat(enrollment.getCheckInAt()).isNotNull();
        assertThat(credential.getCredentialedAt()).isNotNull();
        assertThat(credential.getCredentialedBy()).isEqualTo(actor);
        assertThat(credential.getStation()).isEqualTo("A");
        verify(auditRepository).save(any(AuditLog.class));
    }

    @Test
    void manualCredentialingAlreadyCheckedInIsIdempotent() {
        enrollment.checkIn();
        EventCredential existing = credential(LocalDateTime.now().minusMinutes(2));
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.of(existing));

        CredentialResponse response = service.manual(eventId, actor,
                new com.ccomp.br.domain.events.credential.dto.ManualCredentialRequest(enrollmentId, "B"));

        assertThat(response.result()).isEqualTo("ALREADY_CREDENTIALED");
        verify(credentialRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void manualCredentialingRejectsCanceledEnrollment() {
        enrollment.setStatus(EnumEnrollmentState.CANCELED);
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.manual(eventId, actor,
                new com.ccomp.br.domain.events.credential.dto.ManualCredentialRequest(enrollmentId, "A")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Inscrição não elegível para credenciamento.");
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void manualCredentialingRejectsBannedEnrollment() {
        enrollment.setStatus(EnumEnrollmentState.BANNED);
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.manual(eventId, actor,
                new com.ccomp.br.domain.events.credential.dto.ManualCredentialRequest(enrollmentId, "A")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Inscrição não elegível para credenciamento.");
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void scanWithRotatedTokenIsRejected() {
        String oldToken = "old-token";
        when(credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId, TokenHasher.hash(oldToken)))
                .thenReturn(Optional.of(enrollmentId));
        EventCredential credential = credential(null);
        credential.rotateToken(TokenHasher.hash("new-token"), LocalDateTime.now());
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service.scan(eventId, actor, new CredentialScanRequest(oldToken, "A")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("QR inválido, expirado ou revogado.");
    }

    @Test
    void searchReturnsMaskedEmailAndEligibility() {
        when(enrollmentRepository.searchForCredentialing(eventId, "Ana", PageRequest.of(0, 20)))
                .thenReturn(java.util.List.of(enrollment));
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.empty());

        var results = service.search(eventId, actor, new CredentialSearchRequest(" Ana "));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Ana Souza");
        assertThat(results.getFirst().emailMasked()).isEqualTo("a***@example.org");
        assertThat(results.getFirst().eligible()).isTrue();
        assertThat(results.getFirst().credentialed()).isFalse();
    }

    @Test
    void summaryReturnsEnrollmentAndCredentialCounts() {
        when(enrollmentRepository.countByEventIdAndStatusIn(eq(eventId), any())).thenReturn(7L);
        when(credentialRepository.countByEventIdAndCredentialedAtIsNotNull(eventId)).thenReturn(3L);

        CredentialSummaryResponse summary = service.summary(eventId, actor);

        assertThat(summary).isEqualTo(new CredentialSummaryResponse(7, 3, 4));
    }

    @Test
    void rotatingOwnTokenStoresOnlyItsHash() {
        when(enrollmentRepository.findForCredentialByUserIdAndEventId(actor, eventId)).thenReturn(Optional.of(enrollment));
        when(credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollmentId)).thenReturn(Optional.empty());

        CredentialTokenResponse response = service.rotateMyToken(eventId, actor);

        assertThat(response.token()).isNotBlank();
        assertThat(response.token()).hasSize(43);
        verify(credentialRepository).save(argThat(saved -> TokenHasher.hash(response.token()).equals(saved.getTokenHash())));
        verify(auditRepository).save(any(AuditLog.class));
    }

    private EventCredential credential(LocalDateTime credentialedAt) {
        EventCredential credential = EventCredential.builder().id(900L).eventId(eventId).enrollmentId(enrollmentId)
                .tokenHash(TokenHasher.hash("opaque-token")).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).credentialedAt(credentialedAt).build();
        if (credentialedAt != null) credential.setCredentialedBy(actor);
        return credential;
    }
}

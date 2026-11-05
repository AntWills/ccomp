package com.ccomp.br.domain.events.credential.application;

import com.ccomp.br.domain.auth.passwordreset.application.TokenHasher;
import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
import com.ccomp.br.domain.audit.external.enums.EnumTargetType;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.credential.dto.*;
import com.ccomp.br.domain.events.credential.persistence.*;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventCredentialService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EventCredentialDslRepository credentialDslRepository;
    private final EventCredentialRepository credentialRepository;
    private final EventCredentialOperatorRepository operatorRepository;
    private final UserModelRepository userRepository;
    private final EventAccessPolicy accessPolicy;
    private final AuditLogRepository auditRepository;

    public EventCredentialService(EventRepository eventRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  EventCredentialDslRepository credentialDslRepository,
                                  EventCredentialRepository credentialRepository,
                                  EventCredentialOperatorRepository operatorRepository,
                                  UserModelRepository userRepository,
                                  EventAccessPolicy accessPolicy,
                                  AuditLogRepository auditRepository) {
        this.eventRepository = eventRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.credentialDslRepository = credentialDslRepository;
        this.credentialRepository = credentialRepository;
        this.operatorRepository = operatorRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public CredentialResponse scan(Long eventId, UUID actor, CredentialScanRequest request) {
        assertCanCredential(eventId, actor);
        String hash = TokenHasher.hash(request.token());
        Long enrollmentId = credentialRepository.findEnrollmentIdByEventIdAndTokenHash(eventId, hash)
                .orElseThrow(() -> new ConflictException("QR inválido, expirado ou revogado."));
        Enrollment enrollment = findEnrollment(eventId, enrollmentId);
        return process(enrollment, actor, request.station(), hash);
    }

    @Transactional
    public List<CredentialSearchItem> search(Long eventId, UUID actor, CredentialSearchRequest request) {
        assertCanCredential(eventId, actor);

        String term = request.searchTerm().trim();

        if (term.length() < 2) throw new DomainException("Informe pelo menos dois caracteres.");

        return credentialDslRepository.searchForCredentialing(eventId, term, 20);
    }

    @Transactional
    public CredentialResponse manual(Long eventId, UUID actor, ManualCredentialRequest request) {
        assertCanCredential(eventId, actor);
        Enrollment enrollment = findEnrollment(eventId, request.enrollmentId());
        return process(enrollment, actor, request.station(), null);
    }

    @Transactional
    public CredentialTokenResponse myToken(Long eventId, UUID actor) {
        Event event = findEvent(eventId);
        Enrollment enrollment = enrollmentRepository.findForCredentialByUserIdAndEventId(actor, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada."));
        if (enrollment.getStatus() == EnumEnrollmentState.CANCELED
                || enrollment.getStatus() == EnumEnrollmentState.BANNED) {
            throw new ConflictException("Inscrição não elegível para credenciamento.");
        }
        EventCredential credential = credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollment.getId())
                .orElseGet(() -> newCredential(eventId, enrollment.getId()));
        String raw = null;
        if (credential.getTokenHash() == null) {
            raw = newToken();
            LocalDateTime now = LocalDateTime.now();
            credential.rotateToken(TokenHasher.hash(raw), now);
            credential = credentialRepository.save(credential);
            audit(EnumActionType.CREDENTIAL_TOKEN_ISSUED, actor, credential, now, "token_hash", "issued", null);
        }
        // O token bruto não é recuperável do hash. Na primeira emissão, retornar o token recém-gerado.
        if (raw == null) throw new ConflictException("QR já emitido. Solicite a rotação do código para exibi-lo novamente.");
        return new CredentialTokenResponse(eventId, enrollment.getId(), raw);
    }

    @Transactional
    public CredentialTokenResponse rotateMyToken(Long eventId, UUID actor) {
        Event event = findEvent(eventId);
        Enrollment enrollment = enrollmentRepository.findForCredentialByUserIdAndEventId(actor, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada."));
        return rotateToken(eventId, enrollment, actor);
    }

    @Transactional
    public CredentialTokenResponse rotateParticipantToken(Long eventId, Long enrollmentId, UUID actor) {
        assertCanCredential(eventId, actor);
        return rotateToken(eventId, findEnrollment(eventId, enrollmentId), actor);
    }

    @Transactional
    public CredentialResponse reprint(Long eventId, Long credentialId, UUID actor, String station) {
        assertCanCredential(eventId, actor);
        EventCredential credential = credentialRepository.findById(credentialId)
                .filter(c -> c.getEventId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Credencial não encontrada."));
        if (credential.getCredentialedAt() == null) throw new ConflictException("Participante ainda não foi credenciado.");
        LocalDateTime now = LocalDateTime.now();
        credential.reprint(now);
        audit(EnumActionType.BADGE_REPRINTED, actor, credential, now, "badge_print_count",
                credential.getBadgePrintCount(), station);
        Enrollment enrollment = findEnrollment(eventId, credential.getEnrollmentId());
        return response("ALREADY_CREDENTIALED", enrollment, credential, true);
    }

    @Transactional(readOnly = true)
    public CredentialSummaryResponse summary(Long eventId, UUID actor) {
        assertCanCredential(eventId, actor);

        long eligible = enrollmentRepository.countByEventIdAndStatusIn(eventId,
                List.of(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN));
        long done = credentialRepository.countByEventIdAndCredentialedAtIsNotNull(eventId);
        return new CredentialSummaryResponse(eligible, done, Math.max(0, eligible - done));
    }

    @Transactional
    public void addOperator(Long eventId, UUID userId, UUID actor) {
        assertCanManage(eventId, actor);

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (!user.isAccountValid()) throw new ConflictException("O usuário não possui conta ativa.");
        if (!operatorRepository.existsByEventIdAndUserId(eventId, userId)) {
            operatorRepository.save(EventCredentialOperator.builder().eventId(eventId).userId(userId)
                    .grantedBy(actor).createdAt(LocalDateTime.now()).build());
            auditOperator(EnumActionType.CREDENTIAL_OPERATOR_GRANTED, actor, eventId, userId);
        }
    }

    @Transactional
    public void removeOperator(Long eventId, UUID userId, UUID actor) {
        assertCanManage(eventId, actor);
        operatorRepository.findByEventIdAndUserId(eventId, userId).ifPresent(operator -> {
            operatorRepository.delete(operator);
            auditOperator(EnumActionType.CREDENTIAL_OPERATOR_REVOKED, actor, eventId, userId);
        });
    }

    private CredentialResponse process(Enrollment enrollment, UUID actor, String station, String expectedTokenHash) {
        Long eventId = enrollment.getEventId();
        EventCredential existing = credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollment.getId()).orElse(null);
        if (expectedTokenHash != null && (existing == null || !expectedTokenHash.equals(existing.getTokenHash()))) {
            throw new ConflictException("QR inválido, expirado ou revogado.");
        }
        if (enrollment.getStatus() == EnumEnrollmentState.CANCELED || enrollment.getStatus() == EnumEnrollmentState.BANNED) {
            throw new ConflictException("Inscrição não elegível para credenciamento.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && existing.getCredentialedAt() != null) {
            return response("ALREADY_CREDENTIALED", enrollment, existing, false);
        }
        boolean reconciled = enrollment.getStatus() == EnumEnrollmentState.CHECKED_IN;
        if (!reconciled) enrollment.checkIn();
        EventCredential credential = existing == null ? newCredential(eventId, enrollment.getId()) : existing;
        credential.credential(station, actor, now);
        credentialRepository.save(credential);
        enrollmentRepository.save(enrollment);
        audit(reconciled ? EnumActionType.CREDENTIAL_RECONCILED : EnumActionType.CREDENTIAL_CREATED,
                actor, credential, now, "credentialed_at", now, station);
        return response(reconciled ? "RECONCILED" : "CREDENTIALED", enrollment, credential, true);
    }

    private CredentialResponse response(String result, Enrollment enrollment, EventCredential credential, boolean print) {
        UserModel user = userRepository.findById(enrollment.getUserId()).orElseThrow();
        return new CredentialResponse(result, credential.getId(), enrollment.getId(), enrollment.getStatus().name(),
                credential.getCredentialedAt(), print, user.getName(), maskEmail(user.getEmailAddress().getValue()));
    }

    private EventCredential newCredential(Long eventId, Long enrollmentId) {
        LocalDateTime now = LocalDateTime.now();
        return EventCredential.builder().eventId(eventId).enrollmentId(enrollmentId)
                .createdAt(now).updatedAt(now).build();
    }

    private CredentialTokenResponse rotateToken(Long eventId, Enrollment enrollment, UUID actor) {
        if (enrollment.getStatus() == EnumEnrollmentState.CANCELED || enrollment.getStatus() == EnumEnrollmentState.BANNED)
            throw new ConflictException("Inscrição não elegível para credenciamento.");
        EventCredential credential = credentialRepository.findByEventIdAndEnrollmentId(eventId, enrollment.getId())
                .orElseGet(() -> newCredential(eventId, enrollment.getId()));
        String raw = newToken();
        LocalDateTime now = LocalDateTime.now();
        credential.rotateToken(TokenHasher.hash(raw), now);
        credentialRepository.save(credential);
        audit(EnumActionType.CREDENTIAL_TOKEN_ROTATED, actor, credential, now, "token_hash", "rotated", null);
        return new CredentialTokenResponse(eventId, enrollment.getId(), raw);
    }

    private void audit(EnumActionType action, UUID actor, EventCredential credential, LocalDateTime at,
                       String field, Object value, String station) {
        Map<String, com.ccomp.br.domain.audit.external.dto.ChangeLog> changes = new java.util.HashMap<>();
        changes.put(field, new com.ccomp.br.domain.audit.external.dto.ChangeLog(null, value));
        changes.put("event_id", new com.ccomp.br.domain.audit.external.dto.ChangeLog(null, credential.getEventId()));
        if (station != null) changes.put("station", new com.ccomp.br.domain.audit.external.dto.ChangeLog(null, station));
        auditRepository.save(AuditLog.builder().action(action).actorType(EnumActorType.USER).actorId(actor)
                .targetId(credential.getId().toString()).targetType(EnumTargetType.CREDENTIAL)
                .timestamp(at).reason("Credenciamento de evento")
                .changes(changes).build());
    }

    private void auditOperator(EnumActionType action, UUID actor, Long eventId, UUID operatorId) {
        auditRepository.save(AuditLog.builder().action(action).actorType(EnumActorType.USER).actorId(actor)
                .targetId(operatorId.toString()).targetType(EnumTargetType.CREDENTIAL)
                .timestamp(LocalDateTime.now()).reason("Gestão de operador de credenciamento")
                .changes(Map.of("event_id", new com.ccomp.br.domain.audit.external.dto.ChangeLog(null, eventId)))
                .build());
    }

    private void assertCanCredential(Long eventId, UUID actor) {
        Event event = findEvent(eventId);
        if (operatorRepository.existsByEventIdAndUserId(eventId, actor)) return;
        accessPolicy.assertCanEdit(event, actor);
    }

    private void assertCanManage(Long eventId, UUID actor) {
        Event event = findEvent(eventId);
        accessPolicy.assertCanEdit(event, actor);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private Enrollment findEnrollment(Long eventId, Long enrollmentId) {
        return enrollmentRepository.findByIdAndEventId(enrollmentId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada neste evento."));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }
}

package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.ResolvedActivityCertificateTemplate;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.certificate.Certificate;
import com.ccomp.br.shared.dto.MailAttachment;
import com.ccomp.br.shared.dto.SendMailDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ManualParticipantCertificateServices {
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final CertificateFetcher certificateFetcher;
    private final CertificateVariablesServices variablesServices;
    private final ActivityCache activityCache;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentActivityRepository enrollmentActivityRepository;
    private final UserManagement userManagement;
    private final EmailService emailService;

    public ManualParticipantCertificateServices(EventCache eventCache, EventAccessPolicy accessPolicy,
            CertificateFetcher certificateFetcher, CertificateVariablesServices variablesServices,
            ActivityCache activityCache, EnrollmentRepository enrollmentRepository,
            EnrollmentActivityRepository enrollmentActivityRepository, UserManagement userManagement,
            EmailService emailService) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
        this.activityCache = activityCache;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
        this.userManagement = userManagement;
        this.emailService = emailService;
    }

    public void sendEventCertificateByEmail(Long eventId, Long enrollmentId, UUID requesterId) {
        var certificate = buildEventCertificate(eventId, enrollmentId, requesterId);
        var enrollment = findEnrollment(eventId, enrollmentId);
        var event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        sendCertificate(enrollment.getUserId(), "Certificado do evento: " + event.title(),
                "Olá! Segue em anexo seu certificado do evento <strong>%s</strong>.".formatted(escapeHtml(event.title())),
                "certificado-evento-%d.png".formatted(eventId), certificate);
    }

    public void sendActivityCertificateByEmail(Long eventId, Long activityId, Long enrollmentId, UUID requesterId) {
        var certificate = buildActivityCertificate(eventId, activityId, enrollmentId, requesterId);
        var enrollment = findEnrollment(eventId, enrollmentId);
        var event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        var activity = activityCache.findById(activityId)
                .filter(item -> eventId.equals(item.eventId()))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
        sendCertificate(enrollment.getUserId(), "Certificado da atividade: " + activity.title(),
                "Olá! Segue em anexo seu certificado da atividade <strong>%s</strong>, do evento <strong>%s</strong>."
                        .formatted(escapeHtml(activity.title()), escapeHtml(event.title())),
                "certificado-atividade-%d.png".formatted(activityId), certificate);
    }

    public StorageExternal.StoredFile buildEventCertificate(Long eventId, Long enrollmentId, UUID requesterId) {
        authorize(eventId, requesterId);
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(item -> eventId.equals(item.getEventId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada neste evento."));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var cover = CompletableFuture.supplyAsync(() -> certificateFetcher.fetchCoverTemplate(eventId), executor);
            var box = CompletableFuture.supplyAsync(() -> certificateFetcher.fetchBoxTemplate(eventId), executor);
            var values = box.thenApplyAsync(template -> variablesServices.loadEventVariablesValues(
                    template.getVariables(), eventId, enrollment.getUserId()), executor);
            CompletableFuture.allOf(cover, box, values).join();
            return render(cover.join(), box.join(), values.join());
        } catch (CompletionException e) {
            throw unwrap(e);
        }
    }

    public StorageExternal.StoredFile buildActivityCertificate(Long eventId, Long activityId, Long enrollmentId,
                                                                 UUID requesterId) {
        authorize(eventId, requesterId);
        var activity = activityCache.findById(activityId)
                .filter(item -> eventId.equals(item.eventId()))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(item -> eventId.equals(item.getEventId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada neste evento."));
        var activityEnrollment = enrollmentActivityRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId)
                .filter(item -> !item.isCanceled() && item.haveCheckIn())
                .orElseThrow(() -> new AccessDeniedException("O inscrito não participou desta atividade."));
        if (!activityEnrollment.getEnrollment().haveCheckIn())
            throw new AccessDeniedException("O participante não fez check-in no evento.");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var template = CompletableFuture.supplyAsync(() -> certificateFetcher.resolveActivityTemplate(
                    eventId, activityId, EnumActivityCertificateTemplateType.PARTICIPANT), executor);
            var values = template.thenApplyAsync(resolved -> variablesServices.loadActivityVariablesValues(
                    resolved.variables(), activityId, enrollment.getUserId()), executor);
            CompletableFuture.allOf(template, values).join();
            ResolvedActivityCertificateTemplate resolved = template.join();
            return render(resolved.cover(), resolved.boxes(), values.join());
        } catch (CompletionException e) {
            throw unwrap(e);
        }
    }

    private void authorize(Long eventId, UUID requesterId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!accessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Somente a equipe do evento pode emitir certificados manualmente.");
    }

    private Enrollment findEnrollment(Long eventId, Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .filter(item -> eventId.equals(item.getEventId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada neste evento."));
    }

    private void sendCertificate(UUID recipientId, String subject, String htmlBody, String filename,
                                 StorageExternal.StoredFile certificate) {
        var user = userManagement.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário da inscrição não encontrado."));
        try (var input = certificate.resource().getInputStream()) {
            byte[] content = input.readAllBytes();
            emailService.send(new SendMailDTO(user.emailAddress(), subject, htmlBody, true,
                    List.of(new MailAttachment(filename, content, certificate.contentType()))));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Não foi possível ler o certificado para anexá-lo ao e-mail.", exception);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private StorageExternal.StoredFile render(StorageExternal.StoredFile cover, EventBoxTemplate template,
                                               Map<String, String> values) {
        return render(cover, template.getBoxes(), values);
    }

    private StorageExternal.StoredFile render(StorageExternal.StoredFile cover,
            List<BoxElementDTO> boxes, Map<String, String> values) {
        return Certificate.builder().coverImage(cover)
                .boxElements(boxes.stream().map(BoxElementDTO::toDomain).toList())
                .toReplace(values).build().generate();
    }

    private RuntimeException unwrap(CompletionException exception) {
        if (exception.getCause() instanceof RuntimeException runtimeException) return runtimeException;
        return new IllegalStateException("Falha ao gerar certificado.", exception.getCause());
    }
}

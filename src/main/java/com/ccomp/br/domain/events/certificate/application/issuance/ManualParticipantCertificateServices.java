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
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
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

    public ManualParticipantCertificateServices(EventCache eventCache, EventAccessPolicy accessPolicy,
            CertificateFetcher certificateFetcher, CertificateVariablesServices variablesServices,
            ActivityCache activityCache, EnrollmentRepository enrollmentRepository,
            EnrollmentActivityRepository enrollmentActivityRepository) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
        this.activityCache = activityCache;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
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

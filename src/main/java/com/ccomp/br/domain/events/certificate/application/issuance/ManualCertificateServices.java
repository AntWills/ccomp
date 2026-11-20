package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.CertificateFileName;
import com.ccomp.br.domain.events.certificate.application.GeneratedCertificate;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.ManualInstructorCertificateRequest;
import com.ccomp.br.domain.events.certificate.dto.ManualCertificateContentRequest;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Service
public class ManualCertificateServices {
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final CertificateFetcher certificateFetcher;
    private final CertificateVariablesServices variablesServices;
    private final ActivityCache activityCache;
    private final EventInstructorRepository instructorRepository;
    private final ActivityGuestRepository activityGuestRepository;

    public ManualCertificateServices(
            EventCache eventCache,
            EventAccessPolicy accessPolicy,
            CertificateFetcher certificateFetcher,
            CertificateVariablesServices variablesServices,
            ActivityCache activityCache,
            EventInstructorRepository instructorRepository,
            ActivityGuestRepository activityGuestRepository) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
        this.activityCache = activityCache;
        this.instructorRepository = instructorRepository;
        this.activityGuestRepository = activityGuestRepository;
    }

    public GeneratedCertificate buildInstructorCertificate(
            Long eventId, ManualInstructorCertificateRequest request, UUID requesterId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!accessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Somente a equipe do evento pode emitir certificados manualmente.");

        String instructorName;
        String activityName = null;
        BigDecimal workload;
        boolean guestCertificate = request.instructorId() == null;
        if (request.instructorId() != null) {
            var instructor = instructorRepository.findById(request.instructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ministrante não encontrado."));
            Long targetActivityId = request.activityId() == null ? instructor.getActivityId() : request.activityId();
            if (request.activityId() != null && !request.activityId().equals(instructor.getActivityId()))
                throw new ResourceNotFoundException("Ministrante não está vinculado a esta atividade.");
            var activity = activityCache.findById(targetActivityId)
                    .filter(item -> item.eventId().equals(eventId))
                    .orElseThrow(() -> new ResourceNotFoundException("Ministrante não pertence a este evento."));
            activityName = activity.title();
            if (instructor.getStatus() != EnumInstructorStatus.CONFIRMED)
                throw new AccessDeniedException("O vínculo do ministrante não está confirmado.");
            instructorName = instructor.getName();
            workload = request.activityId() != null
                    ? activity.workloadHours()
                    : instructor.getUserId() == null
                        ? addHours(event.instructorCertificateWorkloadHours(), activity.workloadHours())
                        : variablesServices.sumInstructorWorkload(instructor.getUserId(), eventId);
        } else {
            var activity = activityCache.findById(request.activityId())
                    .filter(item -> item.eventId().equals(eventId))
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
            activityName = activity.title();
            var activityGuest = activityGuestRepository
                    .findByActivityIdAndEventGuestId(request.activityId(), request.eventGuestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Convidado não está vinculado a esta atividade."));
            if (activityGuest.getRole() != EnumActivityGuestRole.PRESENTER
                    || !activityGuest.getEventGuest().isConfirmed())
                throw new AccessDeniedException("Somente apresentadores confirmados podem receber este certificado.");
            instructorName = activityGuest.getEventGuest().getName();
            workload = activityGuest.getEventGuest().getUserId() == null
                    ? addHours(event.guestCertificateWorkloadHours(), activity.workloadHours())
                    : variablesServices.sumGuestWorkload(activityGuest.getEventGuest().getUserId(), eventId);
        }

        if (request.activityId() != null) {
            var context = guestCertificate
                    ? com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType.GUEST
                    : com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType.INSTRUCTOR;
            var resolved = certificateFetcher.resolveActivityTemplate(eventId, request.activityId(), context);
            Map<String, String> values = variablesServices.loadManualActivityRoleVariables(
                    request.activityId(), instructorName, guestCertificate);
            return new GeneratedCertificate(render(resolved.cover(), resolved.boxes(), values),
                    CertificateFileName.create(activityName, instructorName));
        }

        StorageExternal.StoredFile cover = certificateFetcher.fetchCoverTemplate(eventId);
        EventBoxTemplate template = certificateFetcher.fetchBoxTemplate(
                eventId, guestCertificate ? EnumEventCertificateTemplateType.GUEST : EnumEventCertificateTemplateType.INSTRUCTOR);
        Map<String, String> values = guestCertificate
                ? variablesServices.loadManualGuestVariablesValues(eventId, instructorName, workload)
                : variablesServices.loadManualInstructorVariablesValues(eventId, instructorName, workload);
        return new GeneratedCertificate(render(cover, template.getBoxes(), values),
                CertificateFileName.create(event.title(), instructorName));
    }

    public GeneratedCertificate buildTeamCertificate(Long eventId, UUID recipientId, UUID requesterId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Somente a equipe do evento pode emitir certificados manualmente.");
        if (!event.isOwner(recipientId) && !accessPolicy.isActiveEditor(eventId, recipientId))
            throw new AccessDeniedException("O destinatário precisa ser owner ou editor ativo do evento.");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var coverFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchCoverTemplate(eventId), executor);
            var boxFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.EDITOR), executor);
            var variablesFuture = boxFuture.thenApplyAsync(box ->
                    variablesServices.loadEditorVariablesValues(box.getVariables(), eventId, recipientId), executor);

            CompletableFuture.allOf(coverFuture, boxFuture, variablesFuture).join();
            Map<String, String> variables = variablesFuture.join();
            return new GeneratedCertificate(render(coverFuture.join(), boxFuture.join().getBoxes(), variables),
                    CertificateFileName.create(event.title(), variables.get("{equipe.nome}")));
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Falha ao gerar certificado.", e.getCause());
        }
    }

    public GeneratedCertificate previewCustomCertificate(
            Long eventId, UUID requesterId, ManualCertificateContentRequest request) {
        return generateCustomCertificate(eventId, requesterId, request, false);
    }

    public GeneratedCertificate issueCustomCertificate(
            Long eventId, UUID requesterId, ManualCertificateContentRequest request) {
        return generateCustomCertificate(eventId, requesterId, request, true);
    }

    private GeneratedCertificate generateCustomCertificate(
            Long eventId, UUID requesterId, ManualCertificateContentRequest request, boolean definitive) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!accessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Somente a equipe do evento pode gerar certificados manuais.");
        if (definitive && (event.executionStatus() != EnumEventExecutionStatus.FINISHED
                || event.status() == EnumEventStatus.CANCELED))
            throw new DomainException("O certificado definitivo só pode ser emitido após o encerramento do evento.");

        StorageExternal.StoredFile cover = certificateFetcher.fetchCoverTemplate(eventId);
        return new GeneratedCertificate(render(cover, request.boxes(), request.variables()),
                CertificateFileName.create(event.title(), request.variables().get("{participante.nome}")));
    }

    private StorageExternal.StoredFile render(StorageExternal.StoredFile cover, List<BoxElementDTO> boxes,
                                              Map<String, String> variables) {
        return Certificate.builder()
                .coverImage(cover)
                .boxElements(boxes.stream().map(BoxElementDTO::toDomain).toList())
                .toReplace(variables)
                .build()
                .generate();
    }

    private BigDecimal addHours(BigDecimal first, BigDecimal second) {
        return (first == null ? BigDecimal.ZERO : first).add(second == null ? BigDecimal.ZERO : second);
    }
}

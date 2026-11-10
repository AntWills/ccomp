package com.ccomp.br.domain.events.certificate.application.issuance;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.application.CertificateFetcher;
import com.ccomp.br.domain.events.certificate.application.ResolvedActivityCertificateTemplate;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.guests.enums.EnumActivityGuestRole;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuestRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Service
public class AutomaticCertificateServices {
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final CertificateFetcher certificateFetcher;
    private final CertificateVariablesServices variablesServices;
    private final ActivityCache activityCache;
    private final ActivityGuestRepository activityGuestRepository;

    public AutomaticCertificateServices(EventCache eventCache,
                               EventAccessPolicy accessPolicy,
                               CertificateFetcher certificateFetcher,
                               CertificateVariablesServices variablesServices,
                               ActivityCache activityCache,
                               ActivityGuestRepository activityGuestRepository) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
        this.activityCache = activityCache;
        this.activityGuestRepository = activityGuestRepository;
    }

    public StorageExternal.StoredFile buildCertificateFromEvent(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canView(event, userId)) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<StorageExternal.StoredFile> templateFuture =
                    CompletableFuture.supplyAsync(() -> certificateFetcher.fetchCoverTemplate(eventId), executor);

            CompletableFuture<EventBoxTemplate> eventBoxFuture =
                    CompletableFuture.supplyAsync(() -> certificateFetcher.fetchBoxTemplate(eventId), executor);

            CompletableFuture<Map<String, String>> variablesFuture = eventBoxFuture.thenApplyAsync(
                    eventBox -> variablesServices.loadEventVariablesValues(eventBox.getVariables(), eventId, userId),
                    executor
            );

            CompletableFuture.allOf(templateFuture, eventBoxFuture, variablesFuture).join();

            StorageExternal.StoredFile template = templateFuture.join();
            var eventBox = eventBoxFuture.join();
            Map<String, String> variables = variablesFuture.join();

            return render(template, eventBox.getBoxes(), variables);

        } catch (CompletionException e) {
            // Captura o wrapper do CompletableFuture e relança a causa real da falha
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao gerar certificado.", e.getCause());
        }
    }

    public StorageExternal.StoredFile buildCertificateFromActivity(Long activityId, UUID userId) {
        var activity = activityCache.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        Long eventId = activity.eventId();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<ResolvedActivityCertificateTemplate> activityTemplateFuture =
                    CompletableFuture.supplyAsync(() -> certificateFetcher.resolveActivityTemplate(
                            eventId, activityId, EnumActivityCertificateTemplateType.PARTICIPANT), executor);

            CompletableFuture<Map<String, String>> variablesFuture = activityTemplateFuture.thenApplyAsync(
                    activityTemplate -> variablesServices.loadActivityVariablesValues(activityTemplate.variables(), activityId, userId),
                    executor
            );

            CompletableFuture.allOf(activityTemplateFuture, variablesFuture).join();

            var activityTemplate = activityTemplateFuture.join();
            Map<String, String> variables = variablesFuture.join();

            return render(activityTemplate.cover(), activityTemplate.boxes(), variables);

        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao gerar certificado.", e.getCause());
        }
    }

    public StorageExternal.StoredFile buildCertificateFromInstructor(Long eventId, Long instructorId,
                                                                       UUID requesterId) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<UUID> recipientFuture = CompletableFuture.supplyAsync(
                    () -> variablesServices.findInstructorCertificateRecipient(eventId, instructorId), executor);
            CompletableFuture<EventDTO> eventFuture = CompletableFuture.supplyAsync(() -> eventCache.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado.")), executor);
            CompletableFuture.allOf(recipientFuture, eventFuture).join();

            UUID recipientId = recipientFuture.join();
            EventDTO event = eventFuture.join();

            if (!recipientId.equals(requesterId) && !accessPolicy.canEdit(event, requesterId))
                throw new AccessDeniedException("Somente o ministrante ou a equipe do evento pode gerar este certificado.");

            CompletableFuture<StorageExternal.StoredFile> coverFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchCoverTemplate(eventId), executor);
            CompletableFuture<EventBoxTemplate> boxFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.INSTRUCTOR), executor);
            CompletableFuture<Map<String, String>> variablesFuture = boxFuture.thenApplyAsync(box ->
                    variablesServices.loadInstructorVariablesValues(
                            box.getVariables(), eventId, instructorId, recipientId), executor);

            CompletableFuture.allOf(coverFuture, boxFuture, variablesFuture).join();
            return render(coverFuture.join(), boxFuture.join().getBoxes(), variablesFuture.join());
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    public StorageExternal.StoredFile buildCertificateFromActivityGuest(Long eventId, Long activityId,
            Long activityGuestId, UUID requesterId) {
        var activity = activityCache.findById(activityId)
                .filter(item -> item.eventId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));

        var activityGuest = activityGuestRepository.findByActivityIdAndId(activityId, activityGuestId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não está vinculado a esta atividade."));
        var guest = activityGuest.getEventGuest();
        if (activityGuest.getRole() != EnumActivityGuestRole.PRESENTER || !guest.isConfirmed())
            throw new AccessDeniedException("Somente apresentadores confirmados podem emitir este certificado.");
        if (guest.getUserId() == null)
            throw new AccessDeniedException("O convidado precisa vincular uma conta para emitir o próprio certificado.");
        if (!guest.getUserId().equals(requesterId)) {
            EventDTO event = eventCache.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
            if (!accessPolicy.canEdit(event, requesterId))
                throw new AccessDeniedException("Somente o apresentador ou a equipe do evento pode emitir este certificado.");
        }

        StorageExternal.StoredFile cover = certificateFetcher.fetchCoverTemplate(eventId);
        EventBoxTemplate template = certificateFetcher.fetchBoxTemplate(eventId,
                EnumEventCertificateTemplateType.GUEST);
        Map<String, String> values = variablesServices.loadGuestInstructorVariablesValues(eventId, guest.getUserId());
        return render(cover, template.getBoxes(), values);
    }

    public StorageExternal.StoredFile buildActivityCertificateFromInstructor(Long eventId, Long activityId,
            Long instructorId, UUID requesterId) {
        var activity = activityCache.findById(activityId)
                .filter(item -> item.eventId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
        UUID recipient = variablesServices.findActivityInstructorCertificateRecipient(eventId, activityId, instructorId);
        authorizeRecipientOrEditor(eventId, requesterId, recipient,
                "Somente o ministrante ou a equipe do evento pode emitir este certificado.");
        var template = certificateFetcher.resolveActivityTemplate(eventId, activityId,
                EnumActivityCertificateTemplateType.INSTRUCTOR);
        var values = variablesServices.loadActivityRoleVariables(template.variables(), activityId, recipient,
                com.ccomp.br.domain.events.certificate.enums.EnumCertificateType.INSTRUCTOR);
        return render(template.cover(), template.boxes(), values);
    }

    public StorageExternal.StoredFile buildActivityCertificateFromGuest(Long eventId, Long activityId,
            Long activityGuestId, UUID requesterId) {
        activityCache.findById(activityId).filter(item -> item.eventId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada neste evento."));
        var link = activityGuestRepository.findByActivityIdAndId(activityId, activityGuestId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não está vinculado a esta atividade."));
        var guest = link.getEventGuest();
        if (link.getRole() != EnumActivityGuestRole.PRESENTER || !guest.isConfirmed() || guest.getUserId() == null)
            throw new AccessDeniedException("Somente apresentadores confirmados com conta vinculada podem emitir este certificado.");
        authorizeRecipientOrEditor(eventId, requesterId, guest.getUserId(),
                "Somente o apresentador ou a equipe do evento pode emitir este certificado.");
        var template = certificateFetcher.resolveActivityTemplate(eventId, activityId,
                EnumActivityCertificateTemplateType.GUEST);
        var values = variablesServices.loadActivityRoleVariables(template.variables(), activityId, guest.getUserId(),
                com.ccomp.br.domain.events.certificate.enums.EnumCertificateType.GUEST);
        return render(template.cover(), template.boxes(), values);
    }

    private void authorizeRecipientOrEditor(Long eventId, UUID requesterId, UUID recipientId, String message) {
        if (!recipientId.equals(requesterId)) {
            EventDTO event = eventCache.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
            if (!accessPolicy.canEdit(event, requesterId)) throw new AccessDeniedException(message);
        }
    }

    public StorageExternal.StoredFile buildCertificateForEditor(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!event.isOwner(userId) && !accessPolicy.isActiveEditor(eventId, userId))
            throw new AccessDeniedException("Somente owner ou editor ativo pode emitir este certificado.");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<StorageExternal.StoredFile> coverFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchCoverTemplate(eventId), executor);
            CompletableFuture<EventBoxTemplate> boxFuture = CompletableFuture.supplyAsync(
                    () -> certificateFetcher.fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.EDITOR), executor);
            CompletableFuture<Map<String, String>> variablesFuture = boxFuture.thenApplyAsync(box ->
                    variablesServices.loadEditorVariablesValues(box.getVariables(), eventId, userId), executor);

            CompletableFuture.allOf(coverFuture, boxFuture, variablesFuture).join();
            return render(coverFuture.join(), boxFuture.join().getBoxes(), variablesFuture.join());
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    private RuntimeException unwrapCompletionException(CompletionException exception) {
        if (exception.getCause() instanceof RuntimeException runtimeException) return runtimeException;
        return new IllegalStateException("Falha ao gerar certificado.", exception.getCause());
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
}

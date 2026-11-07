package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
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
public class CertificateServices {
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final CertificateFetcher certificateFetcher;
    private final CertificateVariablesServices variablesServices;
    private final ActivityCache activityCache;

    public CertificateServices(EventCache eventCache,
                               EventAccessPolicy accessPolicy,
                               CertificateFetcher certificateFetcher,
                               CertificateVariablesServices variablesServices,
                               ActivityCache activityCache) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
        this.activityCache = activityCache;
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
            CompletableFuture<StorageExternal.StoredFile> templateFuture =
                    CompletableFuture.supplyAsync(() -> certificateFetcher.fetchCoverTemplate(eventId), executor);

            CompletableFuture<EventActivityBoxTemplate> activityBoxFuture =
                    CompletableFuture.supplyAsync(() -> certificateFetcher.fetchActivityBoxTemplate(eventId), executor);

            CompletableFuture<Map<String, String>> variablesFuture = activityBoxFuture.thenApplyAsync(
                    activityBox -> variablesServices.loadActivityVariablesValues(activityBox.getVariables(), activityId, userId),
                    executor
            );

            CompletableFuture.allOf(templateFuture, activityBoxFuture, variablesFuture).join();

            StorageExternal.StoredFile template = templateFuture.join();
            var activityBox = activityBoxFuture.join();
            Map<String, String> variables = variablesFuture.join();

            return render(template, activityBox.getBoxes(), variables);

        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao gerar certificado.", e.getCause());
        }
    }

    public StorageExternal.StoredFile buildCertificateFromInstructor(Long eventId, Long instructorId,
                                                                       UUID requesterId) {
        UUID recipientId = variablesServices.findInstructorCertificateRecipient(eventId, instructorId);
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!recipientId.equals(requesterId) && !accessPolicy.canEdit(event, requesterId)) {
            throw new AccessDeniedException("Somente o ministrante ou a equipe do evento pode gerar este certificado.");
        }
        var cover = certificateFetcher.fetchCoverTemplate(eventId);
        var box = certificateFetcher.fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.INSTRUCTOR);
        var variables = variablesServices.loadInstructorVariablesValues(
                box.getVariables(), eventId, instructorId, recipientId);
        return render(cover, box.getBoxes(), variables);
    }

    public StorageExternal.StoredFile buildCertificateForEditor(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (userId == null || (!event.isOwner(userId) && !accessPolicy.isActiveEditor(eventId, userId))) {
            throw new AccessDeniedException("O certificado está disponível somente para owner ou editor ativo do evento.");
        }
        var cover = certificateFetcher.fetchCoverTemplate(eventId);
        var box = certificateFetcher.fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.EDITOR);
        var variables = variablesServices.loadEditorVariablesValues(box.getVariables(), eventId, userId);
        return render(cover, box.getBoxes(), variables);
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

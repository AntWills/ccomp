package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
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

    public CertificateServices(EventCache eventCache,
                               EventAccessPolicy accessPolicy,
                               CertificateFetcher certificateFetcher,
                               CertificateVariablesServices variablesServices) {
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.certificateFetcher = certificateFetcher;
        this.variablesServices = variablesServices;
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

            // Aguarda a execução paralela
            CompletableFuture.allOf(templateFuture, eventBoxFuture, variablesFuture).join();

            StorageExternal.StoredFile template = templateFuture.join();
            var eventBox = eventBoxFuture.join();
            Map<String, String> variables = variablesFuture.join();

            return Certificate.builder()
                    .coverImage(template)
                    .boxElements(eventBox.getBoxes().stream().map(BoxElementDTO::toDomain).toList())
                    .toReplace(variables)
                    .build()
                    .generate();

        } catch (CompletionException e) {
            // Captura o wrapper do CompletableFuture e relança a causa real da falha
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao gerar certificado.", e.getCause());
        }
    }
}
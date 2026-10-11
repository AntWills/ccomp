package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class CertificateCoverService {
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");
    private final EventCoverTemplateRepository coverTemplateRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final StorageKeyGenerator storageKeyGenerator;
    private final StorageExternal storageExternal;

    public CertificateCoverService(EventCoverTemplateRepository coverTemplateRepository, EventCache eventCache, EventAccessPolicy accessPolicy, StorageKeyGenerator storageKeyGenerator, StorageExternal storageExternal) {
        this.coverTemplateRepository = coverTemplateRepository;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.storageKeyGenerator = storageKeyGenerator;
        this.storageExternal = storageExternal;
    }

    @Transactional
    public StorageExternal.StoredFile loadCoverTemplate(long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        Optional<EventCoverTemplate> coverTemplateOpt = coverTemplateRepository.findByEventId(eventId);
        if (coverTemplateOpt.isEmpty())
            return new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);

        Optional<StorageExternal.StoredFile> objectOpt =
                storageExternal.getObject(coverTemplateOpt.get().getCoverTemplateKey());

        if (objectOpt.isPresent())
            return objectOpt.get();

        // O objeto sumiu do storage: remove a referência órfã e volta ao padrão.
        coverTemplateRepository.deleteByEventId(eventId);
        return new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);
    }

    @Transactional
    public void uploadCoverTemplate(long eventId, UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("Nenhum arquivo enviado.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new DomainException("Tipo de arquivo não suportado. Utilize PNG, JPEG ou Webp.");
        }

        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        String newKey = storageKeyGenerator.generate(
                "events/%d/certificate-cover-template".formatted(eventId),
                file.getOriginalFilename()
        );
        storageExternal.putObject(newKey, file);

        Optional<EventCoverTemplate> coverTemplateOpt = coverTemplateRepository.findByEventId(eventId);

        if(coverTemplateOpt.isEmpty()) {
            coverTemplateRepository.save(
                    EventCoverTemplate.builder()
                            .eventId(eventId)
                            .coverTemplateKey(newKey)
                            .build()
            );
            return;
        }

        EventCoverTemplate coverTemplate = coverTemplateOpt.get();
        String oldKey = coverTemplate.getCoverTemplateKey();
        coverTemplate.setCoverTemplateKey(newKey);

        coverTemplateRepository.save(coverTemplate);
        storageExternal.deleteObject(oldKey);
    }
}

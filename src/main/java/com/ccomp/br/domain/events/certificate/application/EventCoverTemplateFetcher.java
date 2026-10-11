package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class EventCoverTemplateFetcher {

    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");
    private final EventCoverTemplateRepository coverTemplateRepository;
    private final StorageExternal storageExternal;

    public EventCoverTemplateFetcher(EventCoverTemplateRepository coverTemplateRepository,
                                     StorageExternal storageExternal) {
        this.coverTemplateRepository = coverTemplateRepository;
        this.storageExternal = storageExternal;
    }

    @Transactional
    public StorageExternal.StoredFile fetchCoverTemplate(long eventId) {
        Optional<EventCoverTemplate> coverTemplateOpt = coverTemplateRepository.findByEventId(eventId);
        if (coverTemplateOpt.isEmpty()) {
            return new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);
        }

        Optional<StorageExternal.StoredFile> objectOpt =
                storageExternal.getObject(coverTemplateOpt.get().getCoverTemplateKey());

        if (objectOpt.isPresent()) {
            return objectOpt.get();
        }

        coverTemplateRepository.deleteByEventId(eventId);
        return new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);
    }
}
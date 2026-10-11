package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplateRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class CertificateFetcher {

    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");
    private final EventCoverTemplateRepository coverTemplateRepository;
    private final StorageExternal storageExternal;

    private final EventBoxTemplateRepository boxTemplateRepository;

    public CertificateFetcher(EventCoverTemplateRepository coverTemplateRepository,
                              StorageExternal storageExternal,
                              EventBoxTemplateRepository boxTemplateRepository) {
        this.coverTemplateRepository = coverTemplateRepository;
        this.storageExternal = storageExternal;
        this.boxTemplateRepository = boxTemplateRepository;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public EventBoxTemplate fetchBoxTemplate(long eventId) {
        return boxTemplateRepository.findByEventId(eventId)
                .orElse(EventBoxTemplate.builder()
                        .eventId(eventId)
                        .boxes(List.of(
                                new BoxElementDTO(
                                        "Certificamos que *{participante.nome}* participou de *{evento.nome}*, com carga horária de {evento.carga_horaria}.",
                                        0.25, 0.5, 0.5, 25f, null, null
                                ))
                        )
                        .variables(List.of(
                                        "{participante.nome}",
                                        "{evento.nome}",
                                        "{evento.carga_horaria}"
                                )
                        )
                        .build());
    }
}
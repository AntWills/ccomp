package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplateRepository;
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
    private final EventActivityBoxTemplateRepository activityBoxTemplateRepository;

    public CertificateFetcher(EventCoverTemplateRepository coverTemplateRepository,
                              StorageExternal storageExternal,
                              EventBoxTemplateRepository boxTemplateRepository,
                              EventActivityBoxTemplateRepository activityBoxTemplateRepository) {
        this.coverTemplateRepository = coverTemplateRepository;
        this.storageExternal = storageExternal;
        this.boxTemplateRepository = boxTemplateRepository;
        this.activityBoxTemplateRepository = activityBoxTemplateRepository;
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

    @Transactional(readOnly = true)
    public EventBoxTemplate fetchBoxTemplate(long eventId) {
        return fetchBoxTemplate(eventId, EnumEventCertificateTemplateType.PARTICIPANT);
    }

    @Transactional(readOnly = true)
    public EventBoxTemplate fetchBoxTemplate(long eventId, EnumEventCertificateTemplateType templateType) {
        return boxTemplateRepository.findByEventIdAndTemplateType(eventId, templateType)
                .orElseGet(() -> EventBoxTemplate.builder()
                        .eventId(eventId)
                        .templateType(templateType)
                        .boxes(List.of(
                                new BoxElementDTO(
                                        defaultEventCertificateText(templateType),
                                        0.25, 0.5, 0.5, 25f, null, null
                                ))
                        )
                        .variables(defaultEventCertificateVariables(templateType))
                        .build());
    }

    private String defaultEventCertificateText(EnumEventCertificateTemplateType templateType) {
        return switch (templateType) {
            case PARTICIPANT -> "Certificamos que *{participante.nome}* participou de *{evento.title}*, com carga horária de {evento.carga_horaria} horas.";
            case INSTRUCTOR -> "Certificamos que *{ministrante.nome}* atuou como ministrante ou auxiliar em atividade(s) de *{evento.title}*, com carga horária de {ministrante.carga_horaria} horas.";
            case EDITOR -> "Certificamos que *{equipe.nome}* integrou a equipe de *{evento.title}*, com carga horária de {equipe.carga_horaria} horas.";
        };
    }

    private List<String> defaultEventCertificateVariables(EnumEventCertificateTemplateType templateType) {
        return switch (templateType) {
            case PARTICIPANT -> List.of("{participante.nome}", "{evento.title}", "{evento.carga_horaria}");
            case INSTRUCTOR -> List.of("{ministrante.nome}", "{evento.title}", "{ministrante.carga_horaria}");
            case EDITOR -> List.of("{equipe.nome}", "{evento.title}", "{equipe.carga_horaria}");
        };
    }

    @Transactional(readOnly = true)
    public EventActivityBoxTemplate fetchActivityBoxTemplate(long eventId) {
        return activityBoxTemplateRepository.findByEventId(eventId)
                .orElse(EventActivityBoxTemplate.builder()
                        .eventId(eventId)
                        .boxes(List.of(
                                new BoxElementDTO(
                                        "Certificamos que *{participante.nome}* participou da atividade *{atividade.nome}*, " +
                                                "com carga horária de {atividade.carga_horaria} horas.",
                                        0.25, 0.5, 0.5, 25f, null, null
                                )
                        ))
                        .variables(List.of(
                                "{participante.nome}",
                                "{atividade.nome}",
                                "{atividade.carga_horaria}"
                        ))
                        .build());
    }
}

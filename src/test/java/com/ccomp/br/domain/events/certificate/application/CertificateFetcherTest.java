package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.persistence.*;
import com.ccomp.br.domain.storage.external.StorageExternal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateFetcherTest {
    private static final long EVENT_ID = 17L;

    @Mock EventCoverTemplateRepository coverRepository;
    @Mock StorageExternal storage;
    @Mock EventBoxTemplateRepository boxRepository;
    @Mock EventActivityBoxTemplateRepository activityBoxRepository;

    private CertificateFetcher createFetcher() {
        return new CertificateFetcher(coverRepository, storage, boxRepository, activityBoxRepository);
    }

    @Test
    void fetchCoverTemplate_withoutSavedCover_returnsDefaultPng() {
        when(coverRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        StorageExternal.StoredFile result = createFetcher().fetchCoverTemplate(EVENT_ID);

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.resource()).isInstanceOf(ClassPathResource.class);
        assertThat(result.resource().isReadable()).isTrue();
        verifyNoInteractions(storage);
    }

    @Test
    void fetchCoverTemplate_withSavedCover_returnsStoredObject() {
        EventCoverTemplate template = EventCoverTemplate.builder()
                .eventId(EVENT_ID).coverTemplateKey("events/17/cover.png").build();
        StorageExternal.StoredFile stored = new StorageExternal.StoredFile(
                new ByteArrayResource(new byte[]{1, 2, 3}), MediaType.IMAGE_PNG_VALUE);
        when(coverRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(template));
        when(storage.getObject(template.getCoverTemplateKey())).thenReturn(Optional.of(stored));

        assertThat(createFetcher().fetchCoverTemplate(EVENT_ID)).isSameAs(stored);
    }

    @Test
    void fetchCoverTemplate_whenStoredObjectIsMissing_deletesStaleReferenceAndReturnsDefault() {
        EventCoverTemplate template = EventCoverTemplate.builder()
                .eventId(EVENT_ID).coverTemplateKey("events/17/missing.png").build();
        when(coverRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(template));
        when(storage.getObject(template.getCoverTemplateKey())).thenReturn(Optional.empty());

        StorageExternal.StoredFile result = createFetcher().fetchCoverTemplate(EVENT_ID);

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(result.resource()).isInstanceOf(ClassPathResource.class);
        verify(coverRepository).deleteByEventId(EVENT_ID);
    }

    @Test
    void fetchBoxTemplate_withoutSavedTemplate_returnsDefaultTextAndVariables() {
        when(boxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        EventBoxTemplate result = createFetcher().fetchBoxTemplate(EVENT_ID);

        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getVariables()).containsExactly(
                "{participante.nome}", "{evento.title}", "{evento.carga_horaria}");
        assertThat(result.getBoxes()).singleElement().satisfies(box ->
                assertThat(box.textTemplate()).contains("{participante.nome}", "{evento.title}", "{evento.carga_horaria}"));
    }

    @Test
    void fetchActivityBoxTemplate_withoutSavedTemplate_returnsActivityVariables() {
        when(activityBoxRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        EventActivityBoxTemplate result = createFetcher().fetchActivityBoxTemplate(EVENT_ID);

        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getVariables()).containsExactly(
                "{participante.nome}", "{atividade.nome}", "{atividade.carga_horaria}");
        assertThat(result.getBoxes()).singleElement().satisfies(box ->
                assertThat(box.textTemplate()).contains("{participante.nome}", "{atividade.nome}", "{atividade.carga_horaria}"));
    }
}

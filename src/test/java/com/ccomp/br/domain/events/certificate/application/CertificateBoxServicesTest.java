package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplateRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateBoxServicesTest {
    private static final long EVENT_ID = 17L;
    private static final UUID USER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");

    @Mock EventBoxTemplateRepository repository;
    @Mock CertificateFetcher fetcher;
    @Mock EventCache eventCache;
    @Mock EventAccessPolicy accessPolicy;

    private CertificateBoxServices createService() {
        return new CertificateBoxServices(repository, fetcher, eventCache, accessPolicy);
    }

    private EventDTO event() {
        return new EventDTO(EVENT_ID, "Evento", "evento", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, USER_ID, null, null, null);
    }

    @Test
    void loadBoxTemplate_requiresViewAccessAndDelegatesToFetcher() {
        EventDTO event = event();
        EventBoxTemplate template = EventBoxTemplate.builder().eventId(EVENT_ID).build();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canView(event, USER_ID)).thenReturn(true);
        when(fetcher.fetchBoxTemplate(EVENT_ID)).thenReturn(template);

        assertThat(createService().loadBoxTemplate(EVENT_ID, USER_ID)).isSameAs(template);
    }

    @Test
    void loadBoxTemplate_whenEventDoesNotExist_throwsNotFound() {
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().loadBoxTemplate(EVENT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
    }

    @Test
    void saveBoxTemplate_persistsBoxesAndExtractedVariables() {
        EventDTO event = event();
        List<BoxElementDTO> boxes = List.of(box("__{participante.nome}__ em {evento.title}"));
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, USER_ID)).thenReturn(true);
        when(repository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        createService().saveBoxTemplate(EVENT_ID, USER_ID, boxes);

        ArgumentCaptor<EventBoxTemplate> captor = ArgumentCaptor.forClass(EventBoxTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getBoxes()).containsExactlyElementsOf(boxes);
        assertThat(captor.getValue().getVariables()).containsExactly("{participante.nome}", "{evento.title}");
    }

    @Test
    void saveBoxTemplate_reusesExistingTemplate() {
        EventDTO event = event();
        EventBoxTemplate existing = EventBoxTemplate.builder().eventId(EVENT_ID)
                .variables(List.of()).boxes(List.of()).build();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, USER_ID)).thenReturn(true);
        when(repository.findByEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        createService().saveBoxTemplate(EVENT_ID, USER_ID, List.of(box("{evento.title}")));

        verify(repository).save(same(existing));
        assertThat(existing.getVariables()).containsExactly("{evento.title}");
    }

    @Test
    void saveBoxTemplate_rejectsEmptyBoxesAndUnsupportedVariables() {
        EventDTO event = event();
        when(eventCache.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(accessPolicy.canEdit(event, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> createService().saveBoxTemplate(EVENT_ID, USER_ID, List.of()))
                .isInstanceOf(DomainException.class)
                .hasMessage("A lista de caixas não pode estar vazia.");
        assertThatThrownBy(() -> createService().saveBoxTemplate(EVENT_ID, USER_ID, List.of(box("{variavel.invalida}"))))
                .isInstanceOf(DomainException.class)
                .hasMessage("Variável não suportada no template: {variavel.invalida}");
        verify(repository, never()).save(any());
    }

    private BoxElementDTO box(String text) {
        return new BoxElementDTO(text, 0.1, 0.2, 0.8, 24f, "#000000", null);
    }
}

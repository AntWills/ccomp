package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.config.BlazePersistenceConfig;
import com.ccomp.br.domain.events.dto.activities.EventActivityCursor;
import com.ccomp.br.domain.events.dto.activities.EventActivityView;
import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enums.activities.EnumActivityAccessPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationRequirement;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityBlaze;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({BlazePersistenceConfig.class, EventActivityBlaze.class, BlazeQueryExecutor.class})
public class EventActivityTest {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventActivityRepository repository;

    @Autowired
    private EventActivityBlaze eventActivityBlaze;

    @Test
    void shouldThrowExceptionWhenStartDateIsAfterEndDate() {
        Event evento = persistValidEvent();

        EventActivity activity = EventActivity.builder()
                .event(evento)
                .title("Palestra de Abertura")
                .type(EnumActivityType.LECTURE)
                .registrationRequirement(EnumActivityRegistrationRequirement.REQUIRED)
                .accessPolicy(EnumActivityAccessPolicy.PUBLIC)
                .startDate(LocalDateTime.of(2026, 10, 20, 15, 0))
                .endDate(LocalDateTime.of(2026, 10, 20, 14, 0)) // Data final anterior
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(activity))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A data de início não pode ser posterior à data de término.");
    }

    @Test
    void shouldPaginateUsingCursorCorrectly() {
        Event evento = persistValidEvent();

        // Setup: Inserir 3 atividades com tempos diferentes
        LocalDateTime baseTime = LocalDateTime.now();
        EventActivity act1 = saveActivity(evento, "Ativ 1", baseTime);
        EventActivity act2 = saveActivity(evento, "Ativ 2", baseTime.plusHours(1));
        EventActivity act3 = saveActivity(evento, "Ativ 3", baseTime.plusHours(2));

        // Teste: Busca inicial (limit 2)
        List<EventActivityView> page1 = eventActivityBlaze.findByCursor(evento.getId(), null, 2);

        assertThat(page1).hasSize(2);
        // O Blaze deve ordenar do mais recente para o mais antigo (desc)
        assertThat(page1.get(0).getId()).isEqualTo(act3.getId());
        assertThat(page1.get(1).getId()).isEqualTo(act2.getId());

        // Teste: Próxima página (usando o cursor do último item da página 1)
        EventActivityCursor cursor = new EventActivityCursor(act2.getCreatedAt(), act2.getId());
        List<EventActivityView> page2 = eventActivityBlaze.findByCursor(evento.getId(), cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().getId()).isEqualTo(act1.getId());
    }

    private Event persistValidEvent() {
        return eventRepository.save(
            Event.builder()
                    .title("XII Semana da Computação")
                    .slug("xii-semana-da-computacao-7f32c5")
                    .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                    .format(EnumEventFormat.IN_PERSON)
                    .status(EnumEventStatus.DRAFT)
                    .enrollmentPaused(false)
                    .ownerId(UUID.fromString("50d90f1b-3ca0-4e17-a8a6-ee2fd75406d0"))
                    .build()
        );
    }

    private EventActivity saveActivity(Event event, String title, LocalDateTime createdAt) {
        EventActivity activity = EventActivity.builder()
                .event(event)
                .title(title)
                .type(EnumActivityType.LECTURE)
                .registrationRequirement(EnumActivityRegistrationRequirement.REQUIRED)
                .accessPolicy(EnumActivityAccessPolicy.PUBLIC)
                .displayOrder(0L)
                .createdAt(createdAt) // Necessário para a ordenação/cursor do Blaze
                .build();

        return repository.saveAndFlush(activity);
    }
}

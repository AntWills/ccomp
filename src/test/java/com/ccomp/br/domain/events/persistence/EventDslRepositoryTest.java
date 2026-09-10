package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.config.BlazePersistenceConfig;
import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.dto.events.EventCursor;
import com.ccomp.br.domain.events.dto.events.EventListItemDTO;
import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.enums.EnumEventFormat;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        BlazePersistenceConfig.class,
        EventDslRepository.class,
        BlazeQueryExecutor.class,
        QueryDslConfig.class
})
public class EventDslRepositoryTest {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventEditorRepository eventEditorRepository;

    @Autowired
    private EventDslRepository eventDslRepository;

    private UUID editorId;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        editorId = UUID.randomUUID();
        baseTime = LocalDateTime.now();
    }

    @Test
    void shouldPaginateWhereUserIsEditorUsingQueryDsl() {
        // Setup: Inserir 3 eventos com datas crescentes
        Event event1 = persistEventWithEditor("Evento DSL 1", baseTime);
        Event event2 = persistEventWithEditor("Evento DSL 2", baseTime.plusDays(1));
        Event event3 = persistEventWithEditor("Evento DSL 3", baseTime.plusDays(2));

        // Teste: Busca inicial (Página 1 - limit 2)
        List<EventListItemDTO> page1 = eventDslRepository.findAllWhereUserIsEditor(editorId, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).id()).isEqualTo(event3.getId()); // Mais recente
        assertThat(page1.get(1).id()).isEqualTo(event2.getId());

        // Teste: Próxima página (Página 2 usando o cursor do último item)
        EventCursor cursor = new EventCursor(event2.getStartDate(), event2.getId());
        List<EventListItemDTO> page2 = eventDslRepository.findAllWhereUserIsEditor(editorId, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().id()).isEqualTo(event1.getId());
    }

    private Event persistEventWithEditor(String title, LocalDateTime startDate) {
        Event event = eventRepository.saveAndFlush(
                Event.builder()
                        .title(title)
                        .slug(title.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 5))
                        .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                        .format(EnumEventFormat.IN_PERSON)
                        .status(EnumEventStatus.PUBLISHED)
                        .enrollmentPaused(false)
                        .ownerId(UUID.randomUUID())
                        .startDate(startDate)
                        .endDate(startDate.plusHours(2))
                        .build()
        );

        eventEditorRepository.saveAndFlush(
                EventEditor.builder()
                        .event(event)
                        .userId(editorId)
                        .status(EnumEditorsStatus.ACTIVE)
                        .assignedAt(LocalDateTime.now())
                        .build()
        );

        return event;
    }
}

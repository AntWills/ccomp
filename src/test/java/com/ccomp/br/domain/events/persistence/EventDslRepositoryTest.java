package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.core.dto.EventCursor;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventDslRepository;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.events.editors.persistence.EventEditor;
import com.ccomp.br.domain.events.editors.persistence.EventEditorRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, EventDslRepository.class})
public class EventDslRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventEditorRepository eventEditorRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EventDslRepository eventDslRepository;

    private UUID userId;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        baseTime = LocalDateTime.now();
    }

    @Test
    @DisplayName("Deve realizar a paginação de eventos públicos com filtro usando cursor")
    void shouldPaginateFindByCursorUsingQueryDsl() {
        // Arranjo: Inserir eventos publicados em datas distintas
        Event event1 = createEvent("Evento Público 1", baseTime, EnumEventStatus.PUBLISHED, UUID.randomUUID());
        Event event2 = createEvent("Evento Público 2", baseTime.plusDays(1), EnumEventStatus.PUBLISHED, UUID.randomUUID());
        Event event3 = createEvent("Evento Público 3", baseTime.plusDays(2), EnumEventStatus.PUBLISHED, UUID.randomUUID());

        EventsFilterRequest filter = new EventsFilterRequest(
                EnumEventCategory.ACADEMIC_EDUCATIONAL,
                EnumEventFormat.IN_PERSON
        );

        // Ação: Busca da Página 1 (limite 2)
        List<EventListItemDTO> page1 = eventDslRepository.findByCursor(filter, null, 2);

        // Asserções Página 1 (Esperados em ordem decrescente de startDate: event3, depois event2)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).id()).isEqualTo(event3.getId());
        assertThat(page1.get(1).id()).isEqualTo(event2.getId());

        // Ação: Busca da Página 2 usando cursor do último item da Página 1
        EventCursor cursor = new EventCursor(page1.getLast().startDate(), page1.getLast().id());
        List<EventListItemDTO> page2 = eventDslRepository.findByCursor(filter, cursor, 2);

        // Asserções Página 2
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().id()).isEqualTo(event1.getId());
    }

    @Test
    @DisplayName("Deve realizar a paginação de eventos por dono (ownerId) usando cursor")
    void shouldPaginateAllByOwnerIdUsingQueryDsl() {
        // Arranjo: Inserir eventos pertencentes ao mesmo ownerId
        Event event1 = createEvent("Evento Owner 1", baseTime, EnumEventStatus.DRAFT, userId);
        Event event2 = createEvent("Evento Owner 2", baseTime.plusDays(1), EnumEventStatus.PUBLISHED, userId);
        Event event3 = createEvent("Evento Owner 3", baseTime.plusDays(2), EnumEventStatus.PUBLISHED, userId);

        // Ação: Busca da Página 1 (limite 2)
        List<EventListItemDTO> page1 = eventDslRepository.findAllByOwnerId(userId, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).id()).isEqualTo(event3.getId());
        assertThat(page1.get(1).id()).isEqualTo(event2.getId());

        // Ação: Busca da Página 2 usando o cursor
        EventCursor cursor = new EventCursor(page1.getLast().startDate(), page1.getLast().id());
        List<EventListItemDTO> page2 = eventDslRepository.findAllByOwnerId(userId, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().id()).isEqualTo(event1.getId());
    }

    @Test
    @DisplayName("Deve realizar a paginação de inscrições do participante usando cursor")
    void shouldPaginateAllSubscriptionsUsingQueryDsl() {
        // Arranjo: Inserir eventos e relacionar inscrições confirmadas para o participante
        Event event1 = createEvent("Evento Inscrito 1", baseTime, EnumEventStatus.PUBLISHED, UUID.randomUUID());
        Event event2 = createEvent("Evento Inscrito 2", baseTime.plusDays(1), EnumEventStatus.PUBLISHED, UUID.randomUUID());
        Event event3 = createEvent("Evento Inscrito 3", baseTime.plusDays(2), EnumEventStatus.PUBLISHED, UUID.randomUUID());

        enrollUserInEvent(event1, userId, EnumEnrollmentState.CONFIRMED);
        enrollUserInEvent(event2, userId, EnumEnrollmentState.CHECKED_IN);
        enrollUserInEvent(event3, userId, EnumEnrollmentState.CONFIRMED);

        // Ação: Busca da Página 1 (limite 2)
        List<EventListItemDTO> page1 = eventDslRepository.findAllSubscriptions(userId, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).id()).isEqualTo(event3.getId());
        assertThat(page1.get(1).id()).isEqualTo(event2.getId());

        // Ação: Busca da Página 2 usando o cursor
        EventCursor cursor = new EventCursor(page1.getLast().startDate(), page1.getLast().id());
        List<EventListItemDTO> page2 = eventDslRepository.findAllSubscriptions(userId, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().id()).isEqualTo(event1.getId());
    }

    @Test
    @DisplayName("Deve realizar a paginação de eventos onde o usuário é editor usando cursor")
    void shouldPaginateWhereUserIsEditorUsingQueryDsl() {
        // Setup: Inserir 3 eventos com datas crescentes
        Event event1 = persistEventWithEditor("Evento DSL 1", baseTime);
        Event event2 = persistEventWithEditor("Evento DSL 2", baseTime.plusDays(1));
        Event event3 = persistEventWithEditor("Evento DSL 3", baseTime.plusDays(2));

        // Teste: Busca inicial (Página 1 - limit 2)
        List<EventListItemDTO> page1 = eventDslRepository.findAllWhereUserIsEditor(userId, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).id()).isEqualTo(event3.getId()); // Mais recente
        assertThat(page1.get(1).id()).isEqualTo(event2.getId());

        // Teste: Próxima página (Página 2 usando o cursor do último item)
        EventCursor cursor = new EventCursor(page1.getLast().startDate(), page1.getLast().id());
        List<EventListItemDTO> page2 = eventDslRepository.findAllWhereUserIsEditor(userId, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().id()).isEqualTo(event1.getId());
    }

    // --- Auxiliares de Persistência ---

    private Event createEvent(String title, LocalDateTime startDate, EnumEventStatus status, UUID ownerId) {
        return eventRepository.saveAndFlush(
                Event.builder()
                        .title(title)
                        .slug(title.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 5))
                        .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                        .format(EnumEventFormat.IN_PERSON)
                        .status(status)
                        .enrollmentPaused(false)
                        .ownerId(ownerId)
                        .startDate(startDate)
                        .endDate(startDate.plusHours(2))
                        .build()
        );
    }

    private Event persistEventWithEditor(String title, LocalDateTime startDate) {
        Event event = createEvent(title, startDate, EnumEventStatus.PUBLISHED, UUID.randomUUID());

        eventEditorRepository.saveAndFlush(
                EventEditor.builder()
                        .event(event)
                        .userId(userId)
                        .status(EnumEditorsStatus.ACTIVE)
                        .assignedAt(LocalDateTime.now())
                        .build()
        );

        return event;
    }

    private void enrollUserInEvent(Event event, UUID participantId, EnumEnrollmentState state) {
        enrollmentRepository.saveAndFlush(
                Enrollment.builder()
                        .event(event)
                        .userId(participantId)
                        .status(state)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}
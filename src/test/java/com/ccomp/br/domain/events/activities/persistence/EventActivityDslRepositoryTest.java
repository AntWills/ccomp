package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityDTO;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
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
@Import({QueryDslConfig.class, EventActivityDslRepository.class})
class EventActivityDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventActivityDslRepository eventActivityDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de atividades por evento baseada em cursor ordenada por createdAt decrescente")
    void shouldPaginateEventActivitiesUsingCursorInDescendingOrder() {
        // Arranjo
        Event event = Event.builder()
                .title("Evento Teste")
                .slug("evento-teste-odbnoaf")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(event);

        LocalDateTime now = LocalDateTime.now();

        EventActivity activity1 = EventActivity.builder()
                .title("Palestra de Abertura")
                .type(EnumActivityType.LECTURE)
                .displayOrder(2L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now.minusHours(3))
                .event(event)
                .build();

        EventActivity activity2 = EventActivity.builder()
                .title("Workshop de Spring")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(3L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now.minusHours(2))
                .event(event)
                .build();

        EventActivity activity3 = EventActivity.builder()
                .title("Painel de Encerramento")
                .type(EnumActivityType.PANEL)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now.minusHours(1))
                .event(event)
                .build();

        entityManager.persist(activity1);
        entityManager.persist(activity2);
        entityManager.persist(activity3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Primeira página (limite = 2)
        List<EventActivityDTO> page1 = eventActivityDslRepository.findAllByEventIdWithCursor(event.getId(), null, 2);

        // Asserções: Primeira página (deve trazer as 2 mais recentes: activity3 depois activity2)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).title()).isEqualTo("Painel de Encerramento");
        assertThat(page1.get(1).title()).isEqualTo("Palestra de Abertura");

        // Construção do cursor com base no último item da primeira página
        EventActivityDTO lastItem = page1.getLast();
        EventActivityCursor cursor = new EventActivityCursor(lastItem.displayOrder(), lastItem.id());

        // Ação: Segunda página utilizando o cursor (limite = 2)
        List<EventActivityDTO> page2 = eventActivityDslRepository.findAllByEventIdWithCursor(event.getId(), cursor, 2);

        // Asserções: Segunda página (deve trazer a atividade mais antiga: activity1)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().title()).isEqualTo("Workshop de Spring");
    }

    @Test
    @DisplayName("Deve realizar a paginação via cursor de atividades inscritas pelo usuário ordenada por displayOrder crescente e id decrescente")
    void shouldPaginateSubscriptionsUsingCursorInDisplayOrder() {
        // Arranjo
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Simpósio de Tecnologia")
                .slug("simposio-tecnologia-123")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        // Inscrição confirmada do participante no evento
        Enrollment enrollment = Enrollment.builder()
                .event(event)
                .userId(userId)
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now)
                .build();
        entityManager.persist(enrollment);

        // Atividades do evento com ordens de exibição (displayOrder) conhecidas
        EventActivity activity1 = EventActivity.builder()
                .title("Abertura e Boas-Vindas")
                .type(EnumActivityType.LECTURE)
                .displayOrder(10L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        EventActivity activity2 = EventActivity.builder()
                .title("Mesa Redonda sobre Carreira")
                .type(EnumActivityType.PANEL)
                .displayOrder(20L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        EventActivity activity3 = EventActivity.builder()
                .title("Workshop de QueryDSL")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(30L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        entityManager.persist(activity1);
        entityManager.persist(activity2);
        entityManager.persist(activity3);

        // Vínculo do participante com as três atividades
        EnrollmentActivity ea1 = EnrollmentActivity.builder().enrollment(enrollment).activity(activity1).build();
        EnrollmentActivity ea2 = EnrollmentActivity.builder().enrollment(enrollment).activity(activity2).build();
        EnrollmentActivity ea3 = EnrollmentActivity.builder().enrollment(enrollment).activity(activity3).build();

        entityManager.persist(ea1);
        entityManager.persist(ea2);
        entityManager.persist(ea3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página (limite = 2)
        List<EventActivityDTO> page1 = eventActivityDslRepository.findSubscriptionsWithCursor(userId, event.getId(), null, 2);

        // Asserções Página 1: Espera displayOrder 10 (Abertura) e displayOrder 20 (Mesa Redonda)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).title()).isEqualTo("Abertura e Boas-Vindas");
        assertThat(page1.get(1).title()).isEqualTo("Mesa Redonda sobre Carreira");

        // Construção do Cursor a partir do último item retornado na Página 1
        EventActivityDTO lastItem = page1.getLast();
        EventActivityCursor cursor = new EventActivityCursor(lastItem.displayOrder(), lastItem.id());

        // Ação: Busca da Segunda Página com Cursor
        List<EventActivityDTO> page2 = eventActivityDslRepository.findSubscriptionsWithCursor(userId, event.getId(), cursor, 2);

        // Asserções Página 2: Espera displayOrder 30 (Workshop)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().title()).isEqualTo("Workshop de QueryDSL");
    }
}
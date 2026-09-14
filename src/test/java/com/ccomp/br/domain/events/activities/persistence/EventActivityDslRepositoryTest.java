package com.ccomp.br.domain.events.activities.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.dto.EventActivityConflictCursor;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Test
    @DisplayName("Deve verificar se existe conflito de horário para a atividade do participante")
    void shouldCheckIfSchedulingConflictExists() {
        // Arranjo
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Congresso de Tecnologia")
                .slug("congresso-tecnologia-101")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        Enrollment enrollment = Enrollment.builder()
                .event(event)
                .userId(userId)
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now)
                .build();
        entityManager.persist(enrollment);

        // Atividade 1 inscrita: 14:00 às 16:00
        EventActivity activity1 = EventActivity.builder()
                .title("Minicurso Docker")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .startDate(now.withHour(14).withMinute(0))
                .endDate(now.withHour(16).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(activity1);
        entityManager.persist(new EnrollmentActivity(enrollment, activity1));

        // Atividade 2 (alvo de teste - 15:00 às 17:00): Conflita com a atividade 1
        EventActivity conflictingTarget = EventActivity.builder()
                .title("Palestra Kubernetes")
                .type(EnumActivityType.LECTURE)
                .displayOrder(2L)
                .startDate(now.withHour(15).withMinute(0))
                .endDate(now.withHour(17).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(conflictingTarget);

        // Atividade 3 (alvo de teste - 16:00 às 18:00): Sem conflito (inicia quando a 1 termina)
        EventActivity nonConflictingTarget = EventActivity.builder()
                .title("Encerramento")
                .type(EnumActivityType.PANEL)
                .displayOrder(3L)
                .startDate(now.withHour(16).withMinute(0))
                .endDate(now.withHour(18).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(nonConflictingTarget);

        entityManager.flush();
        entityManager.clear();

        // Ação & Asserções
        boolean hasConflict1 = eventActivityDslRepository.existsSchedulingConflict(userId, conflictingTarget);
        boolean hasConflict2 = eventActivityDslRepository.existsSchedulingConflict(userId, nonConflictingTarget);

        assertThat(hasConflict1).isTrue();
        assertThat(hasConflict2).isFalse();
    }

    @Test
    @DisplayName("Deve realizar a paginação de atividades conflitantes via cursor ordenada por startDate crescente")
    void shouldPaginateConflictingActivitiesUsingCursor() {
        // Arranjo
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.of(0, 0)
        );

        Event event = Event.builder()
                .title("Semana de Inovação")
                .slug("semana-inovacao-202")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        Enrollment enrollment = Enrollment.builder()
                .event(event)
                .userId(userId)
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now)
                .build();
        entityManager.persist(enrollment);

        // Atividade alvo abrangente: 13:00 às 20:00
        EventActivity targetActivity = EventActivity.builder()
                .title("Hackathon")
                .type(EnumActivityType.OTHER)
                .displayOrder(0L)
                .startDate(now.withHour(13).withMinute(0))
                .endDate(now.withHour(20).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(targetActivity);

        // Três atividades conflitantes em horários distintos dentro da janela da targetActivity
        EventActivity conflict1 = EventActivity.builder()
                .title("Atividade Conflito A")
                .type(EnumActivityType.LECTURE)
                .displayOrder(1L)
                .startDate(now.withHour(12).withMinute(0))
                .endDate(now.withHour(15).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        EventActivity conflict2 = EventActivity.builder()
                .title("Atividade Conflito B")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(2L)
                .startDate(now.withHour(16).withMinute(0))
                .endDate(now.withHour(17).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        EventActivity conflict3 = EventActivity.builder()
                .title("Atividade Conflito C")
                .type(EnumActivityType.PANEL)
                .displayOrder(3L)
                .startDate(now.withHour(18).withMinute(0))
                .endDate(now.withHour(21).withMinute(0))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();

        entityManager.persist(conflict1);
        entityManager.persist(conflict2);
        entityManager.persist(conflict3);

        // Inscreve o usuário em todas as 3 atividades conflitantes
        entityManager.persist(new EnrollmentActivity(enrollment, conflict1));
        entityManager.persist(new EnrollmentActivity(enrollment, conflict2));
        entityManager.persist(new EnrollmentActivity(enrollment, conflict3));

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página de Conflitos (limite = 2)
        List<EventActivityDTO> page1 = eventActivityDslRepository.findConflictingActivities(userId, targetActivity, null, 2);

        // Asserções Página 1 (em ordem de startDate crescente: 14:00 depois 16:00)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).title()).isEqualTo("Atividade Conflito A");
        assertThat(page1.get(1).title()).isEqualTo("Atividade Conflito B");

        // Construção do Cursor a partir do último item da Página 1
        EventActivityDTO lastItem = page1.getLast();
        EventActivityConflictCursor cursor = new EventActivityConflictCursor(lastItem.id(), lastItem.startDate());

        // Ação: Busca da Segunda Página de Conflitos via Cursor (limite = 2)
        List<EventActivityDTO> page2 = eventActivityDslRepository.findConflictingActivities(userId, targetActivity, cursor, 2);

        // Asserções Página 2 (18:00)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().title()).isEqualTo("Atividade Conflito C");
    }
}
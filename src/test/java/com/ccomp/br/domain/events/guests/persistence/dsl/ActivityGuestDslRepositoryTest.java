package com.ccomp.br.domain.events.guests.persistence.dsl;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.ActivityGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
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
@Import({QueryDslConfig.class, ActivityGuestDslRepository.class})
class ActivityGuestDslRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActivityGuestDslRepository activityGuestDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de convidados de atividade baseada em cursor ordenada por createdAt decrescente")
    void shouldPaginateActivityGuestsUsingCursorInDescendingOrder() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Congresso de Inovação")
                .slug("congresso-inovacao-2026")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        EventActivity activity = EventActivity.builder()
                .title("Workshop de Architecture Clean")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(activity);

        // Cria os convidados base do evento
        EventGuest eg1 = createEventGuest(event, EnumGuestVisibility.PUBLIC, EnumGuestStatus.CONFIRMED, now);
        EventGuest eg2 = createEventGuest(event, EnumGuestVisibility.PUBLIC, EnumGuestStatus.CONFIRMED, now);
        EventGuest eg3 = createEventGuest(event, EnumGuestVisibility.PUBLIC, EnumGuestStatus.CONFIRMED, now);

        // Vínculos dos convidados com a atividade com datas de criação diferentes
        ActivityGuest ag1 = ActivityGuest.builder()
                .activity(activity)
                .eventGuest(eg1)
                .createdAt(now.minusHours(3))
                .build();

        ActivityGuest ag2 = ActivityGuest.builder()
                .activity(activity)
                .eventGuest(eg2)
                .createdAt(now.minusHours(2))
                .build();

        ActivityGuest ag3 = ActivityGuest.builder()
                .activity(activity)
                .eventGuest(eg3)
                .createdAt(now.minusHours(1))
                .build();

        entityManager.persist(ag1);
        entityManager.persist(ag2);
        entityManager.persist(ag3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Primeira página (limite = 2) como Gerente
        List<ActivityGuest> page1 = activityGuestDslRepository.findActivityGuestsWithCursor(activity.getId(), true, null, 2);

        // Asserções Página 1
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).getId()).isEqualTo(ag3.getId());
        assertThat(page1.get(1).getId()).isEqualTo(ag2.getId());

        // Cursor do último item da página 1
        ActivityGuest lastActivityGuest = page1.getLast();
        GuestCursor cursor = new GuestCursor(lastActivityGuest.getId(), lastActivityGuest.getCreatedAt());

        // Ação: Segunda página com Cursor (limite = 2)
        List<ActivityGuest> page2 = activityGuestDslRepository.findActivityGuestsWithCursor(activity.getId(), true, cursor, 2);

        // Asserções Página 2
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().getId()).isEqualTo(ag1.getId());
    }

    @Test
    @DisplayName("Deve filtrar convidados de atividade com base na visibilidade e status do EventGuest quando não for gerente")
    void shouldFilterActivityGuestsBasedOnEventGuestVisibilityAndStatusWhenNotManager() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Simpósio de Software")
                .slug("simposio-software-2026")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        EventActivity activity = EventActivity.builder()
                .title("Palestra Principal")
                .type(EnumActivityType.LECTURE)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .createdAt(now)
                .event(event)
                .build();
        entityManager.persist(activity);

        // Convidados com diferentes privilégios/estados no EventGuest
        EventGuest publicConfirmed = createEventGuest(event, EnumGuestVisibility.PUBLIC, EnumGuestStatus.CONFIRMED, now);
        EventGuest privateGuest = createEventGuest(event, EnumGuestVisibility.PRIVATE, EnumGuestStatus.CONFIRMED, now);
        EventGuest canceledGuest = createEventGuest(event, EnumGuestVisibility.PUBLIC, EnumGuestStatus.CANCELED, now);

        ActivityGuest agPublic = ActivityGuest.builder().activity(activity).eventGuest(publicConfirmed).createdAt(now.minusHours(3)).build();
        ActivityGuest agPrivate = ActivityGuest.builder().activity(activity).eventGuest(privateGuest).createdAt(now.minusHours(2)).build();
        ActivityGuest agCanceled = ActivityGuest.builder().activity(activity).eventGuest(canceledGuest).createdAt(now.minusHours(1)).build();

        entityManager.persist(agPublic);
        entityManager.persist(agPrivate);
        entityManager.persist(agCanceled);

        entityManager.flush();
        entityManager.clear();

        // Ação 1: Consulta por não-gerente
        List<ActivityGuest> publicView = activityGuestDslRepository.findActivityGuestsWithCursor(activity.getId(), false, null, 10);

        // Asserções Não-Gerente (Deve retornar somente o vínculo com EventGuest público e confirmado)
        assertThat(publicView).hasSize(1);
        assertThat(publicView.getFirst().getId()).isEqualTo(agPublic.getId());
        assertThat(publicView.getFirst().getEventGuest().getUserId()).isEqualTo(publicConfirmed.getUserId());

        // Ação 2: Consulta por gerente
        List<ActivityGuest> managerView = activityGuestDslRepository.findActivityGuestsWithCursor(activity.getId(), true, null, 10);

        // Asserções Gerente (Deve retornar todos)
        assertThat(managerView).hasSize(3);
    }

    private EventGuest createEventGuest(Event event, EnumGuestVisibility visibility, EnumGuestStatus status, LocalDateTime createdAt) {
        EventGuest guest = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .visibility(visibility)
                .status(status)
                .createdAt(createdAt)
                .build();
        entityManager.persist(guest);
        return guest;
    }
}
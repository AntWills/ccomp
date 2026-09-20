package com.ccomp.br.domain.events.guests.persistence.dsl;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.guests.dto.GuestCursor;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
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
@Import({QueryDslConfig.class, EventGuestDslRepository.class})
class EventGuestDslRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventGuestDslRepository eventGuestDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de convidados do evento baseada em cursor ordenada por createdAt decrescente")
    void shouldPaginateEventGuestsUsingCursorInDescendingOrder() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Semana de Tecnologia")
                .slug("semana-tecnologia-2026")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        EventGuest guest1 = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(now.minusHours(3))
                .build();

        EventGuest guest2 = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(now.minusHours(2))
                .build();

        EventGuest guest3 = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(now.minusHours(1))
                .build();

        entityManager.persist(guest1);
        entityManager.persist(guest2);
        entityManager.persist(guest3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página como Gerente (limite = 2)
        List<EventGuest> page1 = eventGuestDslRepository.findGuestsWithCursor(event.getId(), true, null, 2);

        // Asserções Página 1 (deve trazer os 2 mais recentes: guest3 depois guest2)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).getId()).isEqualTo(guest3.getId());
        assertThat(page1.get(1).getId()).isEqualTo(guest2.getId());

        // Construção do cursor com base no último item retornado
        EventGuest lastGuest = page1.getLast();
        GuestCursor cursor = new GuestCursor(lastGuest.getId(), lastGuest.getCreatedAt());

        // Ação: Busca da Segunda Página via Cursor (limite = 2)
        List<EventGuest> page2 = eventGuestDslRepository.findGuestsWithCursor(event.getId(), true, cursor, 2);

        // Asserções Página 2 (deve trazer o mais antigo: guest1)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().getId()).isEqualTo(guest1.getId());
    }

    @Test
    @DisplayName("Deve filtrar convidados privados ou cancelados para usuários que não são gerentes")
    void shouldFilterPrivateAndCanceledGuestsForNonManagerUsers() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        Event event = Event.builder()
                .title("Encontro de Desenvolvedores")
                .slug("encontro-devs-2026")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        // Convidado 1: Público e Confirmado (Deve aparecer para todos)
        EventGuest publicConfirmedGuest = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(now.minusHours(3))
                .build();

        // Convidado 2: Privado e Confirmado (Apenas gerentes devem ver)
        EventGuest privateGuest = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CONFIRMED)
                .visibility(EnumGuestVisibility.PRIVATE)
                .createdAt(now.minusHours(2))
                .build();

        // Convidado 3: Público e Cancelado (Apenas gerentes devem ver)
        EventGuest canceledGuest = EventGuest.builder()
                .event(event)
                .userId(UUID.randomUUID())
                .status(EnumGuestStatus.CANCELED)
                .visibility(EnumGuestVisibility.PUBLIC)
                .createdAt(now.minusHours(1))
                .build();

        entityManager.persist(publicConfirmedGuest);
        entityManager.persist(privateGuest);
        entityManager.persist(canceledGuest);

        entityManager.flush();
        entityManager.clear();

        // Ação 1: Consulta por usuário comum (não gerente)
        List<EventGuest> publicView = eventGuestDslRepository.findGuestsWithCursor(event.getId(), false, null, 10);

        // Asserções para Usuário Comum
        assertThat(publicView).hasSize(1);
        assertThat(publicView.getFirst().getId()).isEqualTo(publicConfirmedGuest.getId());

        // Ação 2: Consulta por gerente do evento
        List<EventGuest> managerView = eventGuestDslRepository.findGuestsWithCursor(event.getId(), true, null, 10);

        // Asserções para Gerente
        assertThat(managerView).hasSize(3);
    }
}
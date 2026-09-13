package com.ccomp.br.domain.events.editors.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.editors.dto.EventEditorCursor;
import com.ccomp.br.domain.events.editors.dto.EventEditorListItem;
import com.ccomp.br.domain.events.editors.enums.EnumEditorsStatus;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.module.email.EmailAddress;
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
@Import({QueryDslConfig.class, EventEditorDslRepository.class})
class EventEditorDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventEditorDslRepository eventEditorDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de editores do evento via cursor ordenada por assignedAt e id decrescentes")
    void shouldPaginateEventEditorsUsingCursorInDescendingOrder() {
        // Arranjo - Criando Usuários
        LocalDateTime now = LocalDateTime.now();

        UserModel user1 = UserModel.builder()
                .name("Alice Editora")
                .emailAddress(new EmailAddress("alice.editor@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user2 = UserModel.builder()
                .name("Bob Editor")
                .emailAddress(new EmailAddress("bob.editor@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user3 = UserModel.builder()
                .name("Charlie Editor")
                .emailAddress(new EmailAddress("charlie.editor@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);

        // Arranjo - Criando Evento
        Event event = Event.builder()
                .title("Workshop de Tecnologia")
                .slug("workshop-tecnologia")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        // Arranjo - Atribuindo Editores com diferentes horários de atribuição (assignedAt)
        EventEditor editor1 = EventEditor.builder()
                .event(event)
                .userId(user1.getId())
                .assignedAt(now.minusHours(3)) // Atribuído há 3 horas (Mais antigo)
                .status(EnumEditorsStatus.ACTIVE)
                .build();

        EventEditor editor2 = EventEditor.builder()
                .event(event)
                .userId(user2.getId())
                .assignedAt(now.minusHours(2)) // Atribuído há 2 horas
                .status(EnumEditorsStatus.ACTIVE)
                .build();

        EventEditor editor3 = EventEditor.builder()
                .event(event)
                .userId(user3.getId())
                .assignedAt(now.minusHours(1)) // Atribuído há 1 hora (Mais recente)
                .status(EnumEditorsStatus.ACTIVE)
                .build();

        entityManager.persist(editor1);
        entityManager.persist(editor2);
        entityManager.persist(editor3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página (limite = 2)
        List<EventEditorListItem> page1 = eventEditorDslRepository.findAllWithCursor(event.getId(), null, 2);

        // Asserções Página 1: Espera os 2 mais recentes (editor3 "Charlie", depois editor2 "Bob")
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).user().name()).isEqualTo("Charlie Editor");
        assertThat(page1.get(1).user().name()).isEqualTo("Bob Editor");

        // Construção do Cursor com base no último registro retornado na Página 1
        EventEditorListItem lastItem = page1.getLast();
        EventEditorCursor cursor = new EventEditorCursor(lastItem.assignedAt(), lastItem.id());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<EventEditorListItem> page2 = eventEditorDslRepository.findAllWithCursor(event.getId(), cursor, 2);

        // Asserções Página 2: Espera o último editor restante (editor1 "Alice")
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().user().name()).isEqualTo("Alice Editora");
    }
}
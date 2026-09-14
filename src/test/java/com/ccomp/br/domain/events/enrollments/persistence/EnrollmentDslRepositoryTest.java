package com.ccomp.br.domain.events.enrollments.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentsCursor;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
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
@Import({QueryDslConfig.class, EnrollmentDslRepository.class})
class EnrollmentDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EnrollmentDslRepository enrollmentDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de inscrições via cursor ordenada por createdAt e id decrescentes")
    void shouldPaginateEnrollmentsUsingCursorInDescendingOrder() {
        // Arranjo - Criando Usuários
        LocalDateTime now = LocalDateTime.now();

        UserModel user1 = UserModel.builder()
                .name("Alice Inscrita")
                .emailAddress(new EmailAddress("alice.inscrita@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user2 = UserModel.builder()
                .name("Bob Inscrito")
                .emailAddress(new EmailAddress("bob.inscrito@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user3 = UserModel.builder()
                .name("Charlie Inscrito")
                .emailAddress(new EmailAddress("charlie.inscrito@teste.com"))
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
                .title("Simpósio de Computação")
                .slug("simposio-computacao")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .ownerId(UUID.randomUUID())
                .createdAt(now)
                .build();
        entityManager.persist(event);

        // Arranjo - Criando Inscrições em horários distintos (createdAt)
        Enrollment enrollment1 = Enrollment.builder()
                .event(event)
                .userId(user1.getId())
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now.minusHours(3)) // Inscrição mais antiga
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .event(event)
                .userId(user2.getId())
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now.minusHours(2))
                .build();

        Enrollment enrollment3 = Enrollment.builder()
                .event(event)
                .userId(user3.getId())
                .status(EnumEnrollmentState.CONFIRMED)
                .createdAt(now.minusHours(1)) // Inscrição mais recente
                .build();

        entityManager.persist(enrollment1);
        entityManager.persist(enrollment2);
        entityManager.persist(enrollment3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página (limite = 2)
        List<EnrollmentListItem> page1 = enrollmentDslRepository.findAllWithCursor(event.getId(), null, 2);

        // Asserções Página 1: Espera as 2 mais recentes (enrollment3 "Charlie", depois enrollment2 "Bob")
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).user().name()).isEqualTo("Charlie Inscrito");
        assertThat(page1.get(1).user().name()).isEqualTo("Bob Inscrito");

        // Construção do Cursor com base no último registro retornado na Página 1
        EnrollmentListItem lastItem = page1.getLast();
        EnrollmentsCursor cursor = new EnrollmentsCursor(lastItem.createdAt(), lastItem.id());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<EnrollmentListItem> page2 = enrollmentDslRepository.findAllWithCursor(event.getId(), cursor, 2);

        // Asserções Página 2: Espera a inscrição mais antiga restante (enrollment1 "Alice")
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().user().name()).isEqualTo("Alice Inscrita");
    }
}
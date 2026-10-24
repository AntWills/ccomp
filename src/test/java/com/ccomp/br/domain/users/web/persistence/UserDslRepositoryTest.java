package com.ccomp.br.domain.users.web.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.users.dto.UserCursor;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.dto.UserSearchResult;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.persistence.UserDslRepository;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.roles.Roles;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserItemDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class,UserDslRepository.class})
public class UserDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDslRepository userDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação baseada em cursor ordenada por createdAt decrescente")
    void shouldPaginateUsersUsingCursorInDescendingOrder() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        var user1 = UserModel.builder()
                .name("Alice")
                .emailAddress(new EmailAddress("alice@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now.minusHours(3))
                .updatedAt(now.minusHours(3))
                .build();

        var user2 = UserModel.builder()
                .name("Bob")
                .emailAddress(new EmailAddress("bob@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now.minusHours(2))
                .updatedAt(now.minusHours(2))
                .build();

        var user3 = UserModel.builder()
                .name("Charlie")
                .emailAddress(new EmailAddress("charlie@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.BLOCKED)
                .createdAt(now.minusHours(1))
                .updatedAt(now.minusHours(1))
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);

        var role1 = Roles.builder().user(user1).role(EnumRoles.ADMIN).build();
        var role2 = Roles.builder().user(user2).role(EnumRoles.USER).build();
        var role3 = Roles.builder().user(user3).role(EnumRoles.ADMIN).build();

        entityManager.persist(role1);
        entityManager.persist(role2);
        entityManager.persist(role3);

        entityManager.flush();
        entityManager.clear();


        UserSearchFilter filter = new UserSearchFilter(null, null, null);

        List<UserSearchResult> page1 = userDslRepository.findByCursor(filter, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).user().emailAddress().getValue()).isEqualTo("charlie@teste.com");
        assertThat(page1.get(1).user().emailAddress().getValue()).isEqualTo("bob@teste.com");

        UserItemDTO lastItem = page1.getLast().user();
        UserCursor cursor = new UserCursor(lastItem.createdAt(), lastItem.id());

        List<UserSearchResult> page2 = userDslRepository.findByCursor(filter, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().user().emailAddress().getValue()).isEqualTo("alice@teste.com");
    }

    @Test
    @DisplayName("Deve pesquisar usuários por similaridade no nome ou e-mail com limiar de 40%")
    void shouldSearchUsersByNameOrEmailSimilarity() {
        LocalDateTime now = LocalDateTime.now();
        var nameMatch = createUser("Mariana Silva", "mariana.silva@example.com", now.minusHours(3));
        var emailMatch = createUser("Pessoa Exemplo", "contato@mariana.org", now.minusHours(2));
        var belowThreshold = createUser("Zzzzz", "unrelated@domain.com", now.minusHours(1));

        entityManager.persist(nameMatch);
        entityManager.persist(emailMatch);
        entityManager.persist(belowThreshold);
        entityManager.flush();
        entityManager.persist(Roles.builder().user(nameMatch).role(EnumRoles.USER).build());
        entityManager.persist(Roles.builder().user(emailMatch).role(EnumRoles.USER).build());
        entityManager.persist(Roles.builder().user(belowThreshold).role(EnumRoles.USER).build());
        entityManager.flush();
        entityManager.clear();

        var results = userDslRepository.findByCursor(
                new UserSearchFilter(null, null, "mariana"), null, 10);

        assertThat(results).extracting(result -> result.user().emailAddress().getValue())
                .containsExactlyInAnyOrder("mariana.silva@example.com", "contato@mariana.org");
    }

    @Test
    @DisplayName("Deve ordenar por similaridade e continuar a paginação sem duplicar usuários")
    void shouldOrderAndPaginateBySimilarity() {
        LocalDateTime now = LocalDateTime.now();
        var exact = createUser("Mariana", "exact@domain.com", now.minusHours(3));
        var partial = createUser("Mariana Souza", "partial@domain.com", now.minusHours(2));
        entityManager.persist(exact);
        entityManager.persist(partial);
        entityManager.flush();
        entityManager.persist(Roles.builder().user(exact).role(EnumRoles.USER).build());
        entityManager.persist(Roles.builder().user(partial).role(EnumRoles.USER).build());
        entityManager.flush();
        entityManager.clear();

        var firstPage = userDslRepository.findByCursor(
                new UserSearchFilter(null, null, "Mariana"), null, 1);
        var first = firstPage.getFirst();
        assertThat(first.user().emailAddress().getValue()).isEqualTo("exact@domain.com");

        var cursor = new UserCursor(first.similarity(), first.user().createdAt(), first.user().id());
        var secondPage = userDslRepository.findByCursor(
                new UserSearchFilter(null, null, "Mariana"), cursor, 1);

        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.getFirst().user().emailAddress().getValue()).isEqualTo("partial@domain.com");
    }

    private UserModel createUser(String name, String email, LocalDateTime createdAt) {
        return UserModel.builder()
                .name(name)
                .emailAddress(new EmailAddress(email))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}

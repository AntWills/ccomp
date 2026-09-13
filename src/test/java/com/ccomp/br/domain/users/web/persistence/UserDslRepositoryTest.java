package com.ccomp.br.domain.users.web.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.users.dto.UserCursor;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
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


        UserSearchFilter filter = new UserSearchFilter(null, null);

        List<UserItemDTO> page1 = userDslRepository.findByCursor(filter, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).emailAddress().getValue()).isEqualTo("charlie@teste.com");
        assertThat(page1.get(1).emailAddress().getValue()).isEqualTo("bob@teste.com");

        UserItemDTO lastItem = page1.getLast();
        UserCursor cursor = new UserCursor(lastItem.createdAt(), lastItem.id());

        List<UserItemDTO> page2 = userDslRepository.findByCursor(filter, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().emailAddress().getValue()).isEqualTo("alice@teste.com");
    }
}

package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.clubs.dto.ClubMemberCursor;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.Club;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, ClubMemberDslRepository.class})
class ClubMemberDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClubMemberDslRepository clubMemberDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de membros do clube via cursor ordenada por joinedAt e id decrescentes")
    void shouldPaginateClubMembersUsingCursorInDescendingOrder() {
        // Arranjo - Criando Usuários
        LocalDateTime now = LocalDateTime.now();

        UserModel user1 = UserModel.builder()
                .name("Alice Membro")
                .emailAddress(new EmailAddress("alice.membro@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user2 = UserModel.builder()
                .name("Bob Membro")
                .emailAddress(new EmailAddress("bob.membro@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserModel user3 = UserModel.builder()
                .name("Charlie Membro")
                .emailAddress(new EmailAddress("charlie.membro@teste.com"))
                .password("hash123")
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);

        // Arranjo - Criando Clube
        Club club = Club.builder()
                .name("Clube de IA")
                .summary("Estudos sobre Inteligência Artificial")
                .createdAt(now)
                .build();
        entityManager.persist(club);

        // Arranjo - Atribuindo Membros com datas de ingresso distintas (joinedAt)
        ClubMember member1 = ClubMember.builder()
                .club(club)
                .userId(user1.getId())
                .role(EnumClubMemberRole.MEMBER)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(now.minusHours(3)) // Entrou há 3 horas (Mais antigo)
                .build();

        ClubMember member2 = ClubMember.builder()
                .club(club)
                .userId(user2.getId())
                .role(EnumClubMemberRole.MEMBER)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(now.minusHours(2)) // Entrou há 2 horas
                .build();

        ClubMember member3 = ClubMember.builder()
                .club(club)
                .userId(user3.getId())
                .role(EnumClubMemberRole.INSTRUCTOR)
                .status(EnumClubMemberStatus.ACTIVE)
                .joinedAt(now.minusHours(1)) // Entrou há 1 hora (Mais recente)
                .build();

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);

        entityManager.flush();
        entityManager.clear();

        ClubMemberFilter filter = new ClubMemberFilter(null, null);

        // Ação: Busca da Primeira Página (limite = 2)
        List<ClubMemberListItem> page1 = clubMemberDslRepository.findAllWithCursor(club.getId(), filter, null, 2);

        // Asserções Página 1: Espera os 2 membros que entraram mais recentemente (member3 "Charlie", depois member2 "Bob")
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).user().name()).isEqualTo("Charlie Membro");
        assertThat(page1.get(1).user().name()).isEqualTo("Bob Membro");

        // Construção do Cursor a partir do último item da Página 1
        ClubMemberListItem lastItem = page1.getLast();
        ClubMemberCursor cursor = new ClubMemberCursor(lastItem.id(), lastItem.joinedAt());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<ClubMemberListItem> page2 = clubMemberDslRepository.findAllWithCursor(club.getId(), filter, cursor, 2);

        // Asserções Página 2: Espera o membro restante (member1 "Alice")
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().user().name()).isEqualTo("Alice Membro");
    }
}
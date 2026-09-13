package com.ccomp.br.domain.clubs.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.clubs.dto.ClubCreatedCursor;
import com.ccomp.br.domain.clubs.dto.ClubPublishedCursor;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
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
@Import({QueryDslConfig.class, ClubDslRepository.class})
class ClubDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClubDslRepository clubDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de clubes publicados via cursor ordenada por publishedAt e id decrescentes")
    void shouldPaginatePublishedClubsUsingCursorInDescendingOrder() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        Club club1 = Club.builder()
                .name("Clube de Leitura")
                .summary("Resumo 1")
                .content("Conteúdo 1")
                .createdAt(now.minusDays(5))
                .publishedAt(now.minusHours(3)) // Publicado há 3 horas (Mais antigo)
                .build();

        Club club2 = Club.builder()
                .name("Clube de Programação")
                .summary("Resumo 2")
                .content("Conteúdo 2")
                .createdAt(now.minusDays(4))
                .publishedAt(now.minusHours(2)) // Publicado há 2 horas
                .build();

        Club club3 = Club.builder()
                .name("Clube de Xadrez")
                .summary("Resumo 3")
                .content("Conteúdo 3")
                .createdAt(now.minusDays(3))
                .publishedAt(now.minusHours(1)) // Publicado há 1 hora (Mais recente)
                .build();

        Club unpublishedClub = Club.builder()
                .name("Clube Não Publicado")
                .summary("Rascunho")
                .content("Rascunho de conteúdo")
                .createdAt(now)
                .publishedAt(null) // Não publicado, deve ser ignorado
                .build();

        entityManager.persist(club1);
        entityManager.persist(club2);
        entityManager.persist(club3);
        entityManager.persist(unpublishedClub);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página (limite = 2)
        List<ClubResponseDTO> page1 = clubDslRepository.findAllPublishedWithCursor(null, 2);

        // Asserções Página 1: Espera os 2 mais recentemente publicados (club3, depois club2)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).name()).isEqualTo("Clube de Xadrez");
        assertThat(page1.get(1).name()).isEqualTo("Clube de Programação");

        // Construção do Cursor com base no último registro da Página 1
        ClubResponseDTO lastItem = page1.getLast();
        ClubPublishedCursor cursor = new ClubPublishedCursor(lastItem.id(), lastItem.publishedAt());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<ClubResponseDTO> page2 = clubDslRepository.findAllPublishedWithCursor(cursor, 2);

        // Asserções Página 2: Espera apenas o clube publicado restante (club1)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().name()).isEqualTo("Clube de Leitura");
    }

    @Test
    @DisplayName("Deve realizar a paginação de clubes envolvidos pelo usuário via cursor ordenada por createdAt e id decrescentes")
    void shouldPaginateUserInvolvedClubsUsingCursorInDescendingOrder() {
        // Arranjo
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Club club1 = Club.builder()
                .name("Clube Alfa")
                .summary("Resumo Alfa")
                .content("Conteúdo Alfa")
                .createdAt(now.minusHours(3)) // Criado há 3 horas (Mais antigo)
                .build();

        Club club2 = Club.builder()
                .name("Clube Beta")
                .summary("Resumo Beta")
                .content("Conteúdo Beta")
                .createdAt(now.minusHours(2)) // Criado há 2 horas
                .build();

        Club club3 = Club.builder()
                .name("Clube Gama")
                .summary("Resumo Gama")
                .content("Conteúdo Gama")
                .createdAt(now.minusHours(1)) // Criado há 1 hora (Mais recente)
                .build();

        entityManager.persist(club1);
        entityManager.persist(club2);
        entityManager.persist(club3);

        // Associando o usuário como membro nos 3 clubes
        ClubMember member1 = ClubMember.builder()
                .club(club1)
                .userId(userId)
                .status(EnumClubMemberStatus.ACTIVE)
                .role(EnumClubMemberRole.MEMBER)
                .joinedAt(now)
                .build();

        ClubMember member2 = ClubMember.builder()
                .club(club2)
                .userId(userId)
                .status(EnumClubMemberStatus.ACTIVE)
                .role(EnumClubMemberRole.MEMBER)
                .joinedAt(now)
                .build();

        ClubMember member3 = ClubMember.builder()
                .club(club3)
                .userId(userId)
                .status(EnumClubMemberStatus.ACTIVE)
                .role(EnumClubMemberRole.MEMBER)
                .joinedAt(now)
                .build();

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);

        entityManager.flush();
        entityManager.clear();

        // Ação: Busca da Primeira Página (limite = 2)
        List<ClubResponseDTO> page1 = clubDslRepository.findByUserInvolvedWithCursor(userId, null, 2);

        // Asserções Página 1: Espera os 2 criados mais recentemente (club3 "Gama", depois club2 "Beta")
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).name()).isEqualTo("Clube Gama");
        assertThat(page1.get(1).name()).isEqualTo("Clube Beta");

        // Construção do Cursor com base no último registro retornado na Página 1
        ClubResponseDTO lastItem = page1.getLast();
        ClubCreatedCursor cursor = new ClubCreatedCursor(lastItem.id(), lastItem.createdAt());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<ClubResponseDTO> page2 = clubDslRepository.findByUserInvolvedWithCursor(userId, cursor, 2);

        // Asserções Página 2: Espera o clube restante (club1 "Alfa")
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().name()).isEqualTo("Clube Alfa");
    }
}
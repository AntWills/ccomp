package com.ccomp.br.domain.audit.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
import com.ccomp.br.domain.audit.external.enums.EnumTargetType;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, AuditLogDslRepository.class})
class AuditLogDslRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuditLogDslRepository auditLogDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação de logs de auditoria via cursor ordenada por timestamp e id decrescentes")
    void shouldPaginateAuditLogsUsingCursorInDescendingOrder() {
        // Arranjo
        LocalDateTime now = LocalDateTime.now();
        UUID actorId = UUID.randomUUID();

        AuditLog log1 = AuditLog.builder()
                .action(EnumActionType.USER_UNLOCKED)
                .actorType(EnumActorType.USER)
                .actorId(actorId)
                .targetId("target-123")
                .targetType(EnumTargetType.EVENT)
                .timestamp(now.minusHours(3)) // Mais antigo
                .reason("Atualização inicial")
                .build();

        AuditLog log2 = AuditLog.builder()
                .action(EnumActionType.USER_CHANGE_ROLE)
                .actorType(EnumActorType.USER)
                .actorId(actorId)
                .targetId("target-123")
                .targetType(EnumTargetType.EVENT)
                .timestamp(now.minusHours(2))
                .reason("Segunda atualização")
                .build();

        AuditLog log3 = AuditLog.builder()
                .action(EnumActionType.USER_BLOCKED)
                .actorType(EnumActorType.USER)
                .actorId(actorId)
                .targetId("target-123")
                .targetType(EnumTargetType.EVENT)
                .timestamp(now.minusHours(1)) // Mais recente
                .reason("Remoção do evento")
                .build();

        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(log3);

        entityManager.flush();
        entityManager.clear();

        AuditLogSearchFilter filter = new AuditLogSearchFilter(
                actorId, null, null, null, null
        );

        // Ação: Busca da Primeira Página (limite = 2)
        List<AuditLog> page1 = auditLogDslRepository.findAllWithCursor(filter, null, 2);

        // Asserções Página 1: Espera os 2 logs mais recentes (log3 depois log2)
        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).getReason()).isEqualTo("Remoção do evento");
        assertThat(page1.get(1).getReason()).isEqualTo("Segunda atualização");

        // Construção do Cursor com base no último item retornado da Página 1
        AuditLog lastItem = page1.getLast();
        AuditLogCursor cursor = new AuditLogCursor(lastItem.getTimestamp(), lastItem.getId());

        // Ação: Busca da Segunda Página utilizando o Cursor (limite = 2)
        List<AuditLog> page2 = auditLogDslRepository.findAllWithCursor(filter, cursor, 2);

        // Asserções Página 2: Espera apenas o log restante (log1)
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().getReason()).isEqualTo("Atualização inicial");
    }
}
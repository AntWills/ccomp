package com.ccomp.br.domain.audit.persistence;

import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
import com.ccomp.br.domain.audit.external.enums.EnumTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // Ativa o src/test/resources/application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AuditPersistenceTest {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Deve salvar e buscar AuditLog no Postgres de testes")
    void shouldPersistAuditLog() {
        AuditLog logEntity = AuditLog.builder()
                .action(EnumActionType.USER_BLOCKED)
                .actorType(EnumActorType.SYSTEM)
                .targetId(UUID.randomUUID().toString())
                .targetType(EnumTargetType.USER)
                .timestamp(LocalDateTime.now())
                .build();

        AuditLog saved = auditLogRepository.save(logEntity);

        assertThat(saved.getId()).isNotNull();
        assertThat(auditLogRepository.findById(saved.getId())).isPresent();
    }
}

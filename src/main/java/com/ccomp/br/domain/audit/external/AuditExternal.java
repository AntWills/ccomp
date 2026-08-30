package com.ccomp.br.domain.audit.external;

import com.ccomp.br.domain.audit.external.dto.AuditCommand;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.audit.external.dto.ChangeLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class AuditExternal {
    private final AuditLogRepository auditLogRepository;

    public AuditExternal(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void registerLog(AuditCommand dto) {
        var audiLog = AuditLog.builder()
                .action(dto.actionType())
                .actorType(dto.actorType())
                .actorId(dto.adminId())
                .targetId(dto.targetId().toString())
                .targetType(dto.targetType())
                .reason(dto.reason())
                .changes(
                        Map.of(
                                dto.fieldName(),
                                new ChangeLog(dto.previousStatus(), dto.newStatus())
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);
    }
}

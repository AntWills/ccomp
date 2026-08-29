package com.ccomp.br.domain.audit.external;

import com.ccomp.br.domain.audit.external.dto.AuditBlockUserDTO;
import com.ccomp.br.domain.audit.external.dto.AuditChangerRoleDTO;
import com.ccomp.br.domain.audit.external.dto.AuditUnlockUserDTO;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.audit.persistence.ChangeLog;
import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
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
    public void userBlocked(AuditBlockUserDTO dto) {
        var audiLog = AuditLog.builder()
                .action(EnumActionType.BLOCK.name())
                .actorType(EnumActorType.USER)
                .actorId(dto.adminId())
                .targetId(dto.targetId())
                .reason(dto.reason())
                .changes(
                        Map.of(
                                "status_account",
                                new ChangeLog(dto.previousStatus(), dto.newStatus())
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);
    }

    @Transactional
    public void userUnlock(AuditUnlockUserDTO dto) {
        var audiLog = AuditLog.builder()
                .action(EnumActionType.UNLOCK.name())
                .actorType(EnumActorType.USER)
                .actorId(dto.adminId())
                .targetId(dto.targetId())
                .reason(dto.reason())
                .changes(
                        Map.of(
                                "status_account",
                                new ChangeLog(dto.previousStatus(), dto.newStatus())
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);
    }

    @Transactional
    public void userChangeRole(AuditChangerRoleDTO dto) {
        var audiLog = AuditLog.builder()
                .action(EnumActionType.CHANGE_ROLE.name())
                .actorType(EnumActorType.USER)
                .actorId(dto.adminId())
                .targetId(dto.targetId())
                .changes(
                        Map.of(
                                "role",
                                new ChangeLog(dto.previousStatus(), dto.newStatus())
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);
    }
}

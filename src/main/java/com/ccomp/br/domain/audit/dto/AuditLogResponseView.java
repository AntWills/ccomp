package com.ccomp.br.domain.audit.dto;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.MappingSingular;
import com.ccomp.br.domain.audit.external.enums.EnumActionType;
import com.ccomp.br.domain.audit.external.enums.EnumActorType;
import com.ccomp.br.domain.audit.external.enums.EnumTargetType;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.external.dto.ChangeLog;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@EntityView(AuditLog.class)
public interface AuditLogResponseView {

    @IdMapping
    Long getId();

    EnumActionType getAction();

    EnumActorType getActorType();

    UUID getActorId();

    String getTargetId();

    EnumTargetType getTargetType();

    String getReason();

    @MappingSingular
    Map<String, ChangeLog> getChanges();

    LocalDateTime getTimestamp();
}

package com.ccomp.br.domain.audit.application;

import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLogDslRepository;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogDslRepository auditLogDslRepository;

    public AuditService(AuditLogDslRepository auditLogDslRepository) {
        this.auditLogDslRepository = auditLogDslRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<AuditLog> searchAuditLogs(AuditLogSearchFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        int finalPageSize = pageSize;

        AuditLogCursor decodedCursor = CursorUtils.decode(cursor, AuditLogCursor.class);

        List<AuditLog> results = auditLogDslRepository
                .findAllWithCursor(filter, decodedCursor, finalPageSize + 1);


        return CursorUtils.buildPage(
                results,
                finalPageSize,
                e -> new AuditLogCursor(e.getTimestamp(), e.getId()));
    }
}

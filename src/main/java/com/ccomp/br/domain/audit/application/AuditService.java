package com.ccomp.br.domain.audit.application;

import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.audit.persistence.AuditLogSpec;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogResponse;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<AuditLogResponse> searchAuditLogs(AuditLogSearchFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        int finalPageSize = pageSize;

        AuditLogCursor decodedCursor = CursorUtils.decode(cursor, AuditLogCursor.class).orElse(null);

        List<AuditLogResponse> results = auditLogRepository
                .findBy(
                        AuditLogSpec
                                .buildSpec(filter, decodedCursor),
                        query -> query
                                .sortBy(Sort.by(
                                        Sort.Order.desc("timestamp"),
                                        Sort.Order.desc("id")))
                                .as(AuditLogResponse.class)
                                .limit(finalPageSize)
                                .all()
                );

        return CursorUtils.buildPage(results, finalPageSize, e -> new AuditLogCursor(e.timestamp(), e.id()));
    }
}

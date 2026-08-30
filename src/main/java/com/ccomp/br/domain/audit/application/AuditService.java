package com.ccomp.br.domain.audit.application;

import com.ccomp.br.domain.audit.persistence.AuditLogBlaze;
import com.ccomp.br.domain.audit.persistence.AuditLogRepository;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogResponseView;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogBlaze auditLogBlaze;

    public AuditService(AuditLogRepository auditLogRepository, AuditLogBlaze auditLogBlaze) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogBlaze = auditLogBlaze;
    }

    @Transactional(readOnly = true)
    public CursorPage<AuditLogResponseView> searchAuditLogs(AuditLogSearchFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        int finalPageSize = pageSize;

        AuditLogCursor decodedCursor = CursorUtils.decode(cursor, AuditLogCursor.class).orElse(null);

        List<AuditLogResponseView> results = auditLogBlaze.findByCursor(filter, decodedCursor, finalPageSize + 1);

//        List<AuditLogResponseView> results = auditLogRepository
//                .findBy(
//                        AuditLogSpec
//                                .buildSpec(filter, decodedCursor),
//                        query -> query
//                                .sortBy(Sort.by(
//                                        Sort.Order.desc("timestamp"),
//                                        Sort.Order.desc("id")))
//                                .as(AuditLogResponseView.class)
//                                .limit(finalPageSize)
//                                .all()
//                );

        return CursorUtils.buildPage(results, finalPageSize, e -> new AuditLogCursor(e.getTimestamp(), e.getId()));
    }
}

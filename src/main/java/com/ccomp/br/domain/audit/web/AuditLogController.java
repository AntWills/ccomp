package com.ccomp.br.domain.audit.web;

import com.ccomp.br.domain.audit.application.AuditService;
import com.ccomp.br.domain.audit.dto.AuditLogResponseView;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Logs para Auditoria",
        description = "Endpoints de gestão de logs")
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/users/search")
    @Operation(
            summary = "Listar todos os logs",
            description = "Retorna uma lista, junto com o cursor, de todos os logs da aplicação. Requer permissão ADMIN."
    )
    public ResponseEntity<CursorPage<AuditLogResponseView>> getAuditLogs(
            @Valid @RequestBody AuditLogSearchFilter filter,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(auditService.searchAuditLogs(filter, nextCursor, pageSize));
    }
}

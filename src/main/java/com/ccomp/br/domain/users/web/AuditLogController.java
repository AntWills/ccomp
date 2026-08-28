package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.AdminServices;
import com.ccomp.br.domain.users.dto.AuditLogResponse;
import com.ccomp.br.domain.users.dto.AuditLogSearchFilter;
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
    private final AdminServices adminServices;

    public AuditLogController(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    @PostMapping("/users/search")
    @Operation(
            summary = "Listar todos os logs",
            description = "Retorna uma lista, junto com o cursor, de todos os logs da aplicação. Requer permissão ADMIN."
    )
    public ResponseEntity<CursorPage<AuditLogResponse>> getAuditLogs(
            @Valid @RequestBody AuditLogSearchFilter filter,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(adminServices.searchAuditLogs(filter, nextCursor, pageSize));
    }
}

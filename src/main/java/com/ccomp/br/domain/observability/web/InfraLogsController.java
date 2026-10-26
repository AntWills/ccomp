package com.ccomp.br.domain.observability.web;

import com.ccomp.br.domain.observability.application.InfraLogsService;
import com.ccomp.br.domain.observability.dto.InfraLogsResponse;
import com.ccomp.br.domain.observability.enums.InfraLogLevel;
import com.ccomp.br.domain.observability.enums.InfraLogServiceName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "infra-logs", description = "Logs dos containers coletados pelo Alloy")
@RestController
@RequestMapping("/api/admin/infra-logs")
@PreAuthorize("hasRole('ADMIN')")
public class InfraLogsController {
    private final InfraLogsService infraLogsService;

    public InfraLogsController(InfraLogsService infraLogsService) {
        this.infraLogsService = infraLogsService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar logs de infraestrutura",
            description = "Consulta somente logs de infraestrutura coletados pelo Alloy no Loki. Requer perfil ADMIN. " +
                    "Sem filtros, retorna até 100 linhas da última hora. O filtro container identifica uma instância " +
                    "específica dentro do serviço; use o valor retornado no campo container. O filtro level só retorna " +
                    "resultados para serviços nos quais o Alloy extraiu esse rótulo: proxy, postgres-db e seaweeds. " +
                    "O texto de busca é literal. O período máximo é de 7 dias."
    )
    public InfraLogsResponse search(
            @Parameter(description = "Serviço Compose. Filtra o rótulo service_name do Alloy.",
                    schema = @Schema(type = "string", allowableValues = {
                            "proxy", "ccomp-backend", "postgres-db", "postgres-exporter", "seaweeds",
                            "seaweeds-setup", "rabbitmq", "redis", "redis-exporter", "grafana-lgtm",
                            "prometheus", "alloy"
                    }))
            @RequestParam(required = false) InfraLogServiceName serviceName,
            @Parameter(description = "Nome exato do container, conforme o campo container retornado nos logs. " +
                    "Útil para separar réplicas do mesmo serviço.", example = "ccomp-ccomp-backend-1",
                    schema = @Schema(type = "string", pattern = "^[a-zA-Z0-9_.-]{1,128}$"))
            @RequestParam(required = false) String container,
            @Parameter(description = "Nível normalizado pelo Alloy. Aplicável aos serviços proxy, postgres-db e seaweeds.",
                    schema = @Schema(type = "string", allowableValues = {
                            "info", "warn", "error", "fatal", "panic", "warning", "log", "notice",
                            "debug1", "debug2", "debug3", "debug4", "debug5"
                    }))
            @RequestParam(required = false) InfraLogLevel level,
            @Parameter(description = "Busca literal na mensagem do log; máximo de 256 caracteres.", example = "Connection refused",
                    schema = @Schema(type = "string", maxLength = 256))
            @RequestParam(required = false) String search,
            @Parameter(description = "Início do período em ISO-8601. Padrão: uma hora antes de end.",
                    example = "2026-10-26T12:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "Fim do período em ISO-8601. Padrão: horário atual.",
                    example = "2026-10-26T13:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Parameter(description = "Máximo de linhas retornadas. Padrão 100; mínimo 1 e máximo 500.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "500", defaultValue = "100"))
            @RequestParam(required = false) Integer limit
    ) {
        return infraLogsService.search(
                serviceName == null ? null : serviceName.value(),
                container,
                level == null ? null : level.value(),
                search,
                start,
                end,
                limit
        );
    }
}

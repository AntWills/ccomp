package com.ccomp.br.domain.users.persistence.audit;

import com.ccomp.br.domain.users.enums.EnumActorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Table(
        name = "tb_audit_log",
        indexes = {
                @Index(name = "idx_audit_log_actor_id", columnList = "actor_id"),
                @Index(name = "idx_audit_log_target_id", columnList = "target_id"),
                @Index(name = "idx_audit_log_timestamp", columnList = "timestamp") // Para busca em larga escala.
        }
)
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private EnumActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    private String action;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, ChangeLog> changes;
}

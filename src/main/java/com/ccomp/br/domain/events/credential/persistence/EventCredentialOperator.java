package com.ccomp.br.domain.events.credential.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_event_credential_operators")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCredentialOperator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

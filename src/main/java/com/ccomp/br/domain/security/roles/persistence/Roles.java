package com.ccomp.br.domain.security.roles.persistence;

import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_roles")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(25)")
    private EnumRoles role;
}

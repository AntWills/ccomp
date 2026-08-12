package com.ccomp.br.domain.users.persistence.roles;

import com.ccomp.br.domain.users.enums.EnumRoles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolesRepository extends JpaRepository<Roles, Long> {
    List<Roles> findByUserId(UUID userId);
    Optional<Roles> findByUserIdAndRole(UUID userId, EnumRoles role);
    boolean existsByUserIdAndRole(UUID userId, EnumRoles role);
}
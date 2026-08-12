package com.ccomp.br.domain.users.persistence.roles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolesRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
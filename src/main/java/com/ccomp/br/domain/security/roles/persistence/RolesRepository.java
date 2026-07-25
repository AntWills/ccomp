package com.ccomp.br.domain.security.roles.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolesRepository extends JpaRepository<Roles, Long> {
    List<Roles> findByUserId(UUID userId);
}
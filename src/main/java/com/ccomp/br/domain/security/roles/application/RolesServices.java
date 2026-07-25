package com.ccomp.br.domain.security.roles.application;

import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import com.ccomp.br.domain.security.roles.persistence.Roles;
import com.ccomp.br.domain.security.roles.persistence.RolesRepository;
import com.ccomp.br.domain.users.persistence.UserModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class RolesServices {
    private final RolesRepository rolesRepository;

    public RolesServices(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    @Transactional
    public void setRole(UserModel user, EnumRoles role) {
        Roles roles = Roles.builder()
                .userId(user.getId())
                .role(role)
                .build();

        rolesRepository.save(roles);
    }

    @Transactional(readOnly = true)
    public List<EnumRoles> loadRolesByUserID(UUID userId) {
        return rolesRepository.findByUserId(userId).stream()
                .map(Roles::getRole)
                .toList();
    }
}

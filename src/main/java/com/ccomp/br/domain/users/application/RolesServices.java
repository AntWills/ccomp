package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.users.entity.EnumRoles;
import com.ccomp.br.domain.users.persistence.Roles;
import com.ccomp.br.domain.users.persistence.RolesRepository;
import com.ccomp.br.domain.users.persistence.UserModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}

package com.ccomp.br.domain.users.external;

import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.roles.Roles;
import com.ccomp.br.domain.users.persistence.roles.RolesRepository;
import com.ccomp.br.shared.exceptions.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RolesServices {
    private final RolesRepository rolesRepository;

    public RolesServices(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    @Transactional(readOnly = true)
    public List<EnumRoles> loadRolesByUserID(UUID userId) {
        return rolesRepository.findByUserId(userId).stream()
                .map(Roles::getRole)
                .toList();
    }

    @Transactional
    public void initRole(UserModel user, EnumRoles role) {
        if(rolesRepository.existsByUserId(user.getId()))
            return;

        Roles roles = Roles.builder()
                .user(user)
                .role(role)
                .build();

        rolesRepository.save(roles);
    }

    @Transactional
    public void changeRole(UserModel user, EnumRoles role) {
        Roles entity = rolesRepository.findByUserId(user.getId())
                .orElse(Roles.builder()
                        .user(user)
                        .role(EnumRoles.USER)
                        .build()
                );

        entity.setRole(role);

        rolesRepository.save(entity);
    }
}

package com.ccomp.br.domain.users.external;

import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.persistence.roles.Roles;
import com.ccomp.br.domain.users.persistence.roles.RolesRepository;
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
    public void addRole(UUID userId, EnumRoles role) {
        if(rolesRepository.existsByUserIdAndRole(userId, role))
            return;

        Roles roles = Roles.builder()
                .userId(userId)
                .role(role)
                .build();

        rolesRepository.save(roles);
    }

    @Transactional
    public void removeRole(UUID userId, EnumRoles role) {
        Optional<Roles> entity = rolesRepository.findByUserIdAndRole(userId, role);

        if(entity.isEmpty()) return;

        rolesRepository.delete(entity.get());
    }
}

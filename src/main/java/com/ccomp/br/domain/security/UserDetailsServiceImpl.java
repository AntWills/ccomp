package com.ccomp.br.domain.security;

import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserManagement userManagement;
    private final RolesServices rolesServices;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        UserDTO user = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        List<EnumRoles> roles = rolesServices.loadRolesByUserID(user.id());

        return new UserDetailsImpl(user, roles);
    }
}

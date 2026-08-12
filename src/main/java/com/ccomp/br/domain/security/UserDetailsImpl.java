package com.ccomp.br.domain.security;

import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.shared.dto.UserDTO;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserDetailsImpl implements UserDetails {

    private final UserDTO user;
    private final List<EnumRoles> roles;

    public UserDetailsImpl(UserDTO user, List<EnumRoles> roles) {
        this.user = user;
        this.roles = roles;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    @Override
    public String getPassword() { return user.password(); }

    @Override
    @NonNull
    public String getUsername() { return user.emailAddress().getValue(); }


    public @NonNull UUID getId() { return user.id(); }

    // Conta ativa, não expirada, credenciais válidas
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return user.statusAccount() != EnumUserStatusAccount.BLOCKED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.statusAccount() == EnumUserStatusAccount.ACTIVE; }
}

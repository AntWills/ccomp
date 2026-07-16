package com.ccomp.br.domain.security;

import com.ccomp.br.domain.users.persistence.UserModel;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserDetailsImpl implements UserDetails {

    private final UserModel user;

    public UserDetailsImpl(UserModel user) {
        this.user = user;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Por enquanto role fixa
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    @NonNull
    public String getUsername() { return user.getEmailAddress().getValue(); }


    public @NonNull UUID getId() { return user.getId(); }

    // Conta ativa, não expirada, credenciais válidas
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}

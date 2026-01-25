package com.ccomp.br.domain.auth.application;

import com.ccomp.br.domain.auth.dto.AuthResponse;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.exceptions.BadCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthApplication {
    private final UserManagement userManagement;

    private final JwtService jwtService;

    @Autowired
    public AuthApplication(UserManagement userManagement, JwtService jwtService) {
        this.userManagement = userManagement;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterUserDTO dto) {
        var userDto = userManagement.register(dto);

        return new AuthResponse(jwtService.getAccessToken(userDto.id()),
                jwtService.getExpirationInSeconds());
    }

    public AuthResponse login(LoginRequestDTO dto) {
        var userDTO = userManagement.login(dto.email(), dto.password());

        return new AuthResponse(jwtService.getAccessToken(userDTO.id()), jwtService.getExpirationInSeconds());
    }
}

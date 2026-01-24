package com.comp.br.domain.auth.application;

import com.comp.br.domain.auth.dto.AuthResponse;
import com.comp.br.shared.dto.RegisterUserDTO;
import com.comp.br.domain.users.management.UserManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthApplication {
    private final UserManagement userManagement;

    @Autowired
    public AuthApplication(UserManagement userManagement){
        this.userManagement = userManagement;
    }

    public AuthResponse register(RegisterUserDTO dto){
        var userDto = userManagement.register(dto);

        return new AuthResponse("", 1L); // Terminar depois
    }
}

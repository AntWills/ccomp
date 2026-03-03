package com.ccomp.br.domain.auth.application;

import com.ccomp.br.config.AsyncConfig;
import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.SignInResponse;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.users.management.UserManagement;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthApplication {
    private final UserManagement userManagement;
//    private final
    private final JwtService jwtService;

    @Autowired
    public AuthApplication(UserManagement userManagement, JwtService jwtService) {
        this.userManagement = userManagement;
        this.jwtService = jwtService;
    }

    public void signUp(RegisterUserDTO dto) {
        userManagement.register(dto);
    }

    public SignInResponse signIn(LoginRequestDTO dto) {
        var userDto = userManagement.validateCredentials(dto.email(), dto.password());

        return new SignInResponse(
                jwtService.getAccessToken(userDto.id()),
                jwtService.getRefreshToken(userDto.id()).getToken());
    }

    public Optional<RefreshTokenResponse> refresh(RefreshTokenRequest request){
        return jwtService.validRefreshToken(request)
                .map(RefreshTokenResponse::new);
    }

    @Async(AsyncConfig.VIRTUAL_TASK_EXECUTOR)
    public void logout(RefreshTokenRequest request){
        jwtService.deleteRefreshToken(request);
    }
}

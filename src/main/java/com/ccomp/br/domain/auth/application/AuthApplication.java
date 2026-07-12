package com.ccomp.br.domain.auth.application;

import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.AccessTokenResponse;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.domain.users.security.UserDetailsImpl;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthApplication {
    private final UserManagement userManagement;
//    private final
    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthApplication(UserManagement userManagement, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userManagement = userManagement;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public void signUp(RegisterUserDTO dto) {
        userManagement.register(dto);
    }

    public AccessTokenResponse signIn(LoginRequestDTO dto) {
        var authToken = new UsernamePasswordAuthenticationToken(
                dto.email().getValue(), dto.password()
        );

        Authentication authentication = authenticationManager.authenticate(authToken);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new AccessTokenResponse(
                jwtService.getAccessToken(userDetails.getUser().getId()),
                jwtService.getRefreshToken(userDetails.getUser().getId()).getToken());
    }

    public Optional<RefreshTokenResponse> refresh(RefreshTokenRequest request){
        return jwtService.validRefreshToken(request)
                .map(RefreshTokenResponse::new);
    }

    @Async
    @Transactional
    public void logout(RefreshTokenRequest request){
        jwtService.deleteRefreshToken(request);
    }
}

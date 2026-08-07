package com.ccomp.br.domain.auth.application;

import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.AccessTokenResponse;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.auth.external.dto.PasswordResetRequestedEvent;
import com.ccomp.br.domain.security.jwt.application.JwtService;
import com.ccomp.br.domain.security.passwordreset.application.PasswordResetService;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.domain.security.UserDetailsImpl;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuthApplication {
    private final UserManagement userManagement;
//    private final
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public AuthApplication(UserManagement userManagement, JwtService jwtService, AuthenticationManager authenticationManager, PasswordResetService passwordResetService, ApplicationEventPublisher eventPublisher) {
        this.userManagement = userManagement;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.eventPublisher = eventPublisher;
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

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new AccessTokenResponse(
                jwtService.getAccessToken(userDetails.getId(), roles),
                jwtService.getRefreshToken(userDetails.getId()).getToken());
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

    @Transactional
    public void requestPasswordReset(EmailAddress emailAddress) {
        Optional<UserDTO> userOpt = userManagement.findByEmailAddress(emailAddress);

        if(userOpt.isEmpty()) return;
        var user = userOpt.get();

        String token = passwordResetService.issuePasswordResetToken(user.id());

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.emailAddress(), token));
    }
}

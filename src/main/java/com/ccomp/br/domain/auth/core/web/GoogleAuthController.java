package com.ccomp.br.domain.auth.core.web;

import com.ccomp.br.domain.auth.core.application.AuthApplication;
import com.ccomp.br.domain.auth.core.application.GoogleIdentityVerifier;
import com.ccomp.br.domain.auth.core.dto.ClientMetadataDTO;
import com.ccomp.br.domain.auth.core.dto.GoogleLoginRequest;
import com.ccomp.br.domain.auth.core.dto.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
@RequestMapping("api/auth")
public class GoogleAuthController {
    private final AuthApplication authApplication;
    private final GoogleIdentityVerifier googleIdentityVerifier;

    @PostMapping("/google")
    public TokenPair loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request,
                                    HttpServletRequest servletRequest) {
        var identity = googleIdentityVerifier.verify(request.credential());
        return authApplication.signInWithGoogle(identity.email(), identity.name(),
                ClientMetadataDTO.builder()
                        .ipAddress(servletRequest.getRemoteAddr())
                        .userAgent(servletRequest.getHeader("User-Agent"))
                        .build());
    }
}

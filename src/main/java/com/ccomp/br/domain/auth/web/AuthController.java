package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.application.AuthApplication;
import com.ccomp.br.domain.auth.dto.AuthResponse;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthController {
    private final AuthApplication authApplication;

    @Autowired
    public AuthController(AuthApplication authApplication) {
        this.authApplication = authApplication;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserDTO dto) {
        log.info("Iniciando o registro de um novo usuario.");
        var res = authApplication.register(dto);

        return ResponseEntity.ok(res);
    }
}

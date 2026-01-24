package com.comp.br.domain.auth.web;

import com.comp.br.domain.auth.application.AuthApplication;
import com.comp.br.domain.auth.dto.AuthResponse;
import com.comp.br.shared.dto.RegisterUserDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthApplication authApplication;

    @Autowired
    public AuthController(AuthApplication authApplication) {
        this.authApplication = authApplication;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserDTO dto){
        var res = authApplication.register(dto);

        return ResponseEntity.ok(res);
    }
}

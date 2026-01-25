package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.shared.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("users")
public class UserController {
    private final UserApplication userApplication;

    @Autowired
    public UserController(UserApplication userApplication) {
        this.userApplication = userApplication;
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userApplication.getAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal Jwt jwt) {
        return userApplication.getById(UUID.fromString(jwt.getSubject()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}

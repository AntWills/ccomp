package com.comp.br.domain.users.web;

import com.comp.br.domain.users.dto.CreateUserDTO;
import com.comp.br.domain.users.dto.UserRes;
import com.comp.br.domain.users.persistence.UserModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("users")
public class UserController {
    private List<UserModel> users = new ArrayList<>();

    @GetMapping("/{id}")
    public ResponseEntity<UserRes> getById(@PathVariable UUID id){
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(user -> ResponseEntity.ok(
                        new UserRes(user.getId(), user.getName(), user.getEmailAddress())
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserRes>> getAll(){
        return ResponseEntity.ok(users.stream()
                .map(
                        user -> new UserRes(user.getId(), user.getName(), user.getEmailAddress())
                ).toList());
    }

    @PostMapping
    public ResponseEntity<UserRes> create(@Valid @RequestBody CreateUserDTO dto){
        var user = new UserModel(UUID.randomUUID(), dto.name(), dto.email());
        this.users.add(user);

        return ResponseEntity.ok(new UserRes(user.getId(), user.getName(), user.getEmailAddress()));
    }
}

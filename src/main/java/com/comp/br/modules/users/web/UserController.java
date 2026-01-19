package com.comp.br.modules.users.web;

import com.comp.br.modules.users.dto.UserRes;
import com.comp.br.modules.users.persistence.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/users")
public class UserController {
    private List<UserModel> users = new ArrayList<>();

    @GetMapping("/{id}")
    public ResponseEntity<UserRes> getById(@PathVariable UUID id){
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(user -> ResponseEntity.ok(
                        new UserRes(user.getId(), user.getName())
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserRes>> getAll(){
        return ResponseEntity.ok(users.stream()
                .map(
                        user -> new UserRes(user.getId(), user.getName())
                ).toList());
    }

    @PostMapping
    public ResponseEntity<UserRes> create(){
        var user = new UserModel(UUID.fromString("019bd7f5-9dc5-728e-b7bf-ff238a6f5e0f"), "Wills");
        this.users.add(user);

        return ResponseEntity.ok(new UserRes(user.getId(), user.getName()));
    }
}

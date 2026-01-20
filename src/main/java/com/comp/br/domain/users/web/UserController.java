package com.comp.br.domain.users.web;

import com.comp.br.domain.users.application.UserApplication;
import com.comp.br.domain.users.dto.CreateUserDTO;
import com.comp.br.domain.users.dto.UserRes;
import com.comp.br.domain.users.persistence.UserModel;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("users")
public class UserController {


    private final UserApplication userApplication;

    @Autowired
    public UserController(UserApplication userApplication){
        this.userApplication = userApplication;
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserRes>> getAll(){
        return ResponseEntity.ok(userApplication.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRes> getById(@PathVariable UUID id){
        var res = userApplication.getById(id);

        return res.isPresent() ?
                ResponseEntity.ok(res.get())
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<UserRes> create(@Valid @RequestBody CreateUserDTO dto){
        return ResponseEntity.ok(userApplication.create(dto));
    }
}

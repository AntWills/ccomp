package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.shared.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<UserDTO>> getAll(){
        return ResponseEntity.ok(userApplication.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable UUID id){
        var res = userApplication.getById(id);

        return res.isPresent() ?
                ResponseEntity.ok(res.get())
                : ResponseEntity.notFound().build();
    }
}

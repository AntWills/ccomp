package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(name = "tb_users")
@Entity(name = "UserModel")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private EmailAddress emailAddress;

    public UserModel(String name, EmailAddress email){
        this.name = name;
        this.emailAddress = email;
    }
}

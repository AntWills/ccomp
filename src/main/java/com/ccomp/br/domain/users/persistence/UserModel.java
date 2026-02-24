package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "tb_users")
@Entity(name = "UserModel")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private EmailAddress emailAddress;

    @Column(nullable = false)
    private String password;

    public UserModel(String name, String password, EmailAddress email){
        this.name = name;
        this.password = password;
        this.emailAddress = email;
    }
}

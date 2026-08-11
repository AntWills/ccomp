package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status_account", columnDefinition = "varchar(25)", nullable = false)
    private EnumUserStatusAccount statusAccount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserModel(String name, String password, EmailAddress email){
        this.name = name;
        this.password = password;
        this.emailAddress = email;
    }

    public void deactivate() {
        this.statusAccount = EnumUserStatusAccount.DEACTIVATED;
    }

    public void block() {
        this.statusAccount = EnumUserStatusAccount.BLOCKED;
    }

    public void unlock() {
        if(statusAccount == EnumUserStatusAccount.BLOCKED)
            this.statusAccount = EnumUserStatusAccount.ACTIVE;
    }
}

package com.comp.br.domain.users.persistence;

import com.comp.br.module.email.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserModelRepository extends JpaRepository<UserModel, UUID> {
    Optional<UserModel> findByEmailAddress(EmailAddress emailAddress);
}
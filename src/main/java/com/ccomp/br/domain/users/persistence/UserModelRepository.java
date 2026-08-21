package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserModelRepository extends JpaRepository<UserModel, UUID>, JpaSpecificationExecutor<UserModel> {
    Optional<UserModel> findByEmailAddress(EmailAddress emailAddress);
    List<UserSummaryView> findAllByIdIn(List<UUID> ids);
    boolean existsById(UUID id);
    boolean existsByEmailAddress(EmailAddress emailAddress);
}
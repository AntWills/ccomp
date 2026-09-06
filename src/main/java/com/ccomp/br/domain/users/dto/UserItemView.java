package com.ccomp.br.domain.users.dto;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.module.email.EmailAddress;

import java.time.LocalDateTime;
import java.util.UUID;

@EntityView(UserModel.class)
public interface UserItemView {

    @IdMapping
    UUID getId();

    String getName();

    EmailAddress getEmailAddress();

    EnumUserStatusAccount getStatusAccount();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    @Mapping("role.role")
    EnumRoles getRole();
}

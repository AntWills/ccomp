package com.ccomp.br.domain.users.util;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.shared.dto.UserDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper {
    @Mapping(target = "role", source = "role.role")
    UserDTO userToDto(UserModel model);
}

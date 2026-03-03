package com.ccomp.br.domain.users.util;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.shared.dto.UserDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO userToDto(UserModel model);
}

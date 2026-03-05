package com.ccomp.br.domain.users.util;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.shared.dto.UserDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper {
    UserDTO userToDto(UserModel model);
}

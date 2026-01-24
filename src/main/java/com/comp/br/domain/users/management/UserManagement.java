package com.comp.br.domain.users.management;

import com.comp.br.domain.users.persistence.UserModel;
import com.comp.br.domain.users.persistence.UserModelRepository;
import com.comp.br.shared.dto.RegisterUserDTO;
import com.comp.br.shared.dto.UserDTO;
import com.comp.br.shared.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserManagement {
    private final UserModelRepository userModelRepository;

    @Autowired
    public UserManagement(UserModelRepository userModelRepository){
        this.userModelRepository = userModelRepository;
    }

    public UserDTO register(RegisterUserDTO dto){
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if(exist.isPresent()) throw new ConflictException("Exist email!");

        UserModel user = new UserModel(dto.name(), dto.email());
        userModelRepository.save(user);

        return new UserDTO(user.getId(), user.getName(), user.getEmailAddress());
    }
}

package com.ccomp.br.domain.users.application;

import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserApplication {
    private final UserModelRepository userModelRepository;

    @Autowired
    public UserApplication(UserModelRepository userModelRepository){
        this.userModelRepository = userModelRepository;
    }

    public Optional<UserDTO> getById(UUID id){
        return userModelRepository.findById(id)
                .map(userModel -> new UserDTO(userModel.getId(), userModel.getName(), userModel.getEmailAddress()));
    }


    public List<UserDTO> getAll(){
        return userModelRepository.findAll()
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmailAddress()))
                .toList();
    }
}

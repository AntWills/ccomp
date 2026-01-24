package com.comp.br.domain.users.application;

import com.comp.br.shared.dto.UserDTO;
import com.comp.br.domain.users.persistence.UserModelRepository;
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
        var user = userModelRepository.findById(id);

        return user.isPresent() ?
                Optional.of(new UserDTO(
                        user.get().getId(),
                        user.get().getName(),
                        user.get().getEmailAddress()))
                : Optional.empty();
    }


    public List<UserDTO> getAll(){
        return userModelRepository.findAll()
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmailAddress()))
                .toList();
    }
}

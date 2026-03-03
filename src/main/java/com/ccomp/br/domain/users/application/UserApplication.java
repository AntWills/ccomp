package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.users.util.UserMapper;
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
    private final UserMapper userMapper;

    @Autowired
    public UserApplication(UserModelRepository userModelRepository, UserMapper userMapper){
        this.userModelRepository = userModelRepository;
        this.userMapper = userMapper;
    }

    public Optional<UserDTO> getById(UUID id){
        return userModelRepository.findById(id)
                .map(userMapper::userToDto);
    }


    public List<UserDTO> getAll(){
        return userModelRepository.findAll()
                .stream()
                .map(userMapper::userToDto)
                .toList();
    }
}

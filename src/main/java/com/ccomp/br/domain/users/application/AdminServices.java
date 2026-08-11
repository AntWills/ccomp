package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AdminServices {
    private final UserModelRepository userModelRepository;
    private final UserMapper userMapper;

    @Autowired
    public AdminServices(UserModelRepository userModelRepository, UserMapper userMapper){
        this.userModelRepository = userModelRepository;
        this.userMapper = userMapper;
    }


    public Optional<UserDTO> getByEmail(EmailAddress email){
        log.info("Buscando no banco os dados do email: {}", email);
        return userModelRepository.findByEmailAddress(email)
                .map(userMapper::userToDto);
    }
}

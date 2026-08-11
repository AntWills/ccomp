package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.DebugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public List<UserDTO> getAll(){
        return userModelRepository.findAll()
                .stream()
                .map(userMapper::userToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> getByEmail(EmailAddress email){
        log.info("Buscando no banco os dados do email: {}", email);
        return userModelRepository.findByEmailAddress(email)
                .map(userMapper::userToDto);
    }

    @Transactional
    public void blockUser(UUID userId, String reason, UUID adminId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário com id [%s] não encontrado.".formatted(userId)));

        user.block();

        userModelRepository.save(user);

    }

    @Transactional
    public void unlockUser(UUID userId, String reason, UUID adminId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário com id [%s] não encontrado.".formatted(userId)));

//        user.unlock();

        log.info("Dados do usuário após unlock:\n{}", DebugUtils.printJson(user));

        userModelRepository.save(user);
    }
}

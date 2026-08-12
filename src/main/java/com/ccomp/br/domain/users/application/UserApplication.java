package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.security.jwt.application.JwtService;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserApplication {
    private final UserModelRepository userModelRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Autowired
    public UserApplication(UserModelRepository userModelRepository, UserMapper userMapper, JwtService jwtService){
        this.userModelRepository = userModelRepository;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    public Optional<UserDTO> getById(UUID id){
        log.info("Buscando no banco os dados do userId: {}", id);
        return userModelRepository.findById(id)
                .map(userMapper::userToDto);
    }

    @Transactional
    public void deactivateOwnAccount(UUID userId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        user.deactivate(); // Regra de domínio

        userModelRepository.save(user);

        // Efeitos colaterais específicos de auto-desativação:
        // - Revogar tokens JWT ativos
        // - Enviar e-mail de confirmação de desativação
        jwtService.deleteRefreshTokenByUserId(userId);
    }
}

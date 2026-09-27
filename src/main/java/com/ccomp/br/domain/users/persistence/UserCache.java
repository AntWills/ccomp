package com.ccomp.br.domain.users.persistence;


import com.ccomp.br.domain.users.utils.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.cache.CacheNames;
import com.ccomp.br.shared.dto.UserDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserCache {

    private final UserModelRepository userModelRepository;
    private final UserMapper userMapper;

    public UserCache(UserModelRepository userModelRepository, UserMapper userMapper) {
        this.userModelRepository = userModelRepository;
        this.userMapper = userMapper;
    }

    @Cacheable(cacheNames = CacheNames.USERS_BY_ID, key = "#id")
    public Optional<UserDTO> findById(UUID id) {
        return userModelRepository.findById(id)
                .map(userMapper::userToDto);
    }

    @Cacheable(cacheNames = CacheNames.USERS_BY_EMAIL, key = "#emailAddress.value.toLowerCase()")
    public Optional<UserDTO> findByEmailAddress(EmailAddress emailAddress) {
        return userModelRepository.findByEmailAddress(emailAddress)
                .map(userMapper::userToDto);
    }

    /**
     * Invalida as duas "vistas" cacheadas do mesmo usuário de uma vez.
     * Recebe o UserModel já carregado para não precisar consultar o banco de novo
     * só pra saber o email correspondente ao ID.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.USERS_BY_ID, key = "#user.id"),
            @CacheEvict(cacheNames = CacheNames.USERS_BY_EMAIL, key = "#user.emailAddress.value.toLowerCase()")
    })
    public void evict(UserModel user) {
    }
}

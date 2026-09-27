package com.ccomp.br.domain.users.external;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.users.persistence.UserCache;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.external.dto.UserCreatedMessageDTO;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.utils.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserManagement {
    private final RabbitTemplate rabbitTemplate;
    private final RolesServices rolesServices;
    private final UserModelRepository userModelRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserCache userCache;

    @Autowired
    public UserManagement(RabbitTemplate rabbitTemplate, RolesServices rolesServices, UserModelRepository userModelRepository, PasswordEncoder passwordEncoder, UserMapper userMapper, UserCache userCache){
        this.rabbitTemplate = rabbitTemplate;
        this.rolesServices = rolesServices;
        this.userModelRepository = userModelRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userCache = userCache;
    }
    @Transactional
    public void register(RegisterUserDTO dto) {
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if (exist.isPresent()) throw new ConflictException("Já existe uma conta com esses dados.");

        String encryptedPassword = passwordEncoder.encode(dto.password());

        UserModel user = UserModel.builder()
                .name(dto.name())
                .emailAddress(dto.email())
                .password(encryptedPassword)
                .statusAccount(EnumUserStatusAccount.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserModel userSaved = userModelRepository.save(user);
        rolesServices.initRole(userSaved, EnumRoles.USER);

        userCache.evict(userSaved); // chamada EXTERNA a outro bean, passa pelo proxy normalmente

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_USER_CREATED,
                new UserCreatedMessageDTO(dto.name(), dto.email().getValue())
        );
    }

    @Transactional
    public void reactivateAccount(UUID userId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario não encontrado."));
        user.activate();
        userModelRepository.save(user);

        userCache.evict(user);
    }

    public boolean isAccountActive(UUID userId) {
        return userModelRepository.findById(userId)
                .map(user -> user.getStatusAccount() == EnumUserStatusAccount.ACTIVE)
                .orElse(false);
    }

    public void updatePassword(UUID userId, String password) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario não encontrado."));

        String encryptedPassword = passwordEncoder.encode(password);
        user.setPassword(encryptedPassword);
        userModelRepository.save(user);

        userCache.evict(user);
    }

    public Optional<UserDTO> findById(UUID id) {
        return userCache.findById(id); // delega ao componente de cache
    }

    public Optional<UserDTO> findByEmailAddress(EmailAddress emailAddress) {
        return userCache.findByEmailAddress(emailAddress);
    }

    /** Used by Spring Security, which needs the password hash from UserDTO. */
    public Optional<UserDTO> findByEmailAddressForAuthentication(EmailAddress emailAddress) {
        return userModelRepository.findByEmailAddress(emailAddress)
                .map(userMapper::userToDto);
    }

    public List<UserSummaryView> findAllSummaryByIds(List<UUID> ids) {
        return userModelRepository.findAllByIdIn(ids);
    }
}

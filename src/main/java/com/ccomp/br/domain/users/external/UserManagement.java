package com.ccomp.br.domain.users.external;

import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.external.dto.UserCreatedEvent;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserManagement {
    private final ApplicationEventPublisher eventPublisher;
    private final RolesServices rolesServices;
    private final UserModelRepository userModelRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Autowired
    public UserManagement(ApplicationEventPublisher eventPublisher, RolesServices rolesServices, UserModelRepository userModelRepository, PasswordEncoder passwordEncoder, UserMapper userMapper){
        this.eventPublisher = eventPublisher;
        this.rolesServices = rolesServices;
        this.userModelRepository = userModelRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public void register(RegisterUserDTO dto){
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if(exist.isPresent()) throw new ConflictException("Exist email!");

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
        rolesServices.addRole(userSaved.getId(), EnumRoles.USER);

        eventPublisher.publishEvent(new UserCreatedEvent(dto.name(), dto.email()));
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
    }

    public Optional<UserDTO> findById(UUID id){
        return userModelRepository.findById(id)
                .map(userMapper::userToDto);
    }

    public Optional<UserDTO> findByEmailAddress(EmailAddress emailAddress){
        return userModelRepository.findByEmailAddress(emailAddress)
                .map(userMapper::userToDto);
    }

    public List<UserDTO> findAllByIds(List<UUID> ids) {
        return userModelRepository.findAllById(ids)
                .stream()
                .map(userMapper::userToDto)
                .toList();
    }

    public boolean userExists(UUID id){
        return userModelRepository.existsById(id);
    }
}

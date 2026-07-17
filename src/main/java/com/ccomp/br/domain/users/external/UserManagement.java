package com.ccomp.br.domain.users.external;

import com.ccomp.br.domain.users.application.RolesServices;
import com.ccomp.br.domain.users.dto.UserCreatedEvent;
import com.ccomp.br.domain.users.entity.EnumRoles;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.BadCredentialsException;
import com.ccomp.br.shared.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.REQUIRED)
    public void register(RegisterUserDTO dto){
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if(exist.isPresent()) throw new ConflictException("Exist email!");

        String encryptedPassword = passwordEncoder.encode(dto.password());

        UserModel userSaved = userModelRepository.save(new UserModel(dto.name(), encryptedPassword, dto.email()));
        rolesServices.setRole(userSaved, EnumRoles.USER);

        eventPublisher.publishEvent(new UserCreatedEvent(dto.name(), dto.email()));
    }

    public UserDTO validateCredentials(EmailAddress emailAddress, String password) {
        return userModelRepository.findByEmailAddress(emailAddress)
                .filter(userModel -> passwordEncoder.matches(password, userModel.getPassword()))
                .map(userMapper::userToDto)
                .orElseThrow(() -> new BadCredentialsException("Email or password incorrect!"));
    }

    public Optional<UserDTO> findById(UUID id){
        return userModelRepository.findById(id)
                .map(userMapper::userToDto);
    }

    public boolean userExists(UUID id){
        return userModelRepository.existsById(id);
    }
}

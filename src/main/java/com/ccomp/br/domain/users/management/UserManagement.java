package com.ccomp.br.domain.users.management;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.BadCredentialsException;
import com.ccomp.br.shared.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserManagement {
    private final UserModelRepository userModelRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Autowired
    public UserManagement(UserModelRepository userModelRepository, PasswordEncoder passwordEncoder, UserMapper userMapper){
        this.userModelRepository = userModelRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public void register(RegisterUserDTO dto){
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if(exist.isPresent()) throw new ConflictException("Exist email!");

        String encryptedPassword = passwordEncoder.encode(dto.password());

        userModelRepository.save(new UserModel(dto.name(), encryptedPassword, dto.email()));
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
}

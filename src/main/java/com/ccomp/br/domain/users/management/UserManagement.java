package com.ccomp.br.domain.users.management;

import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.BadCredentialsException;
import com.ccomp.br.shared.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserManagement {
    private final UserModelRepository userModelRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserManagement(UserModelRepository userModelRepository, PasswordEncoder passwordEncoder){
        this.userModelRepository = userModelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO register(RegisterUserDTO dto){
        var exist = userModelRepository.findByEmailAddress(dto.email());
        if(exist.isPresent()) throw new ConflictException("Exist email!");

        String encryptedPassword = passwordEncoder.encode(dto.password());

        UserModel user = new UserModel(dto.name(), encryptedPassword, dto.email());
        userModelRepository.save(user);

        return new UserDTO(user.getId(), user.getName(), user.getEmailAddress());
    }

    public UserDTO login(EmailAddress emailAddress, String password) {
        return userModelRepository.findByEmailAddress(emailAddress)
                .filter(userModel -> passwordEncoder.matches(password, userModel.getPassword()))
                .map(userModel -> new UserDTO(userModel.getId(), userModel.getName(), userModel.getEmailAddress()))
                .orElseThrow(() -> new BadCredentialsException("Email or password incorrect!"));
    }
}

package com.comp.br.domain.users.application;

import com.comp.br.domain.users.dto.CreateUserDTO;
import com.comp.br.domain.users.dto.UserRes;
import com.comp.br.domain.users.persistence.UserModel;
import com.comp.br.domain.users.persistence.UserModelRepository;
import com.comp.br.shared.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserApplication {
    private final UserModelRepository userModelRepository;

    @Autowired
    public UserApplication(UserModelRepository userModelRepository){
        this.userModelRepository = userModelRepository;
    }

    public Optional<UserRes> getById(UUID id){
        var user = userModelRepository.findById(id);

        return user.isPresent() ?
                Optional.of(new UserRes(
                        user.get().getId(),
                        user.get().getName(),
                        user.get().getEmailAddress()))
                : Optional.empty();
    }

    public UserRes create(CreateUserDTO dto){
        if(userModelRepository.findByEmailAddress(dto.email()).isPresent())
            throw new ConflictException("Exist already email.");

        var user = new UserModel(dto.name(), dto.email());

        user = userModelRepository.save(user);

        return new UserRes(user.getId(), user.getName(), user.getEmailAddress());
    }


    public List<UserRes> getAll(){
        return userModelRepository.findAll()
                .stream()
                .map(user -> new UserRes(user.getId(), user.getName(), user.getEmailAddress()))
                .toList();
    }
}

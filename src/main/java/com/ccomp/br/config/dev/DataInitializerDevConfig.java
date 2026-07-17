package com.ccomp.br.config.dev;

import com.ccomp.br.domain.users.application.RolesServices;
import com.ccomp.br.domain.users.entity.EnumRoles;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.module.email.EmailAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@Profile("dev")
public class DataInitializerDevConfig {
    @Bean
    CommandLineRunner initUserAdmin(UserModelRepository userModelRepository,
                                    RolesServices rolesServices,
                                    PasswordEncoder encoder) {
        return args -> {
            EmailAddress emailAddress = new EmailAddress("admin@gmail.com");

            if (!userModelRepository.existsByEmailAddress(emailAddress)) {
                var userSaved = userModelRepository.save(UserModel.builder()
                        .name("admin")
                        .emailAddress(emailAddress)
                        .password(encoder.encode("admin"))
                        .build());

                rolesServices.setRole(userSaved, EnumRoles.ADMIN);

                log.info("Usuário admin criado com sucesso.\nEmail: {}\nPassword: {}", emailAddress.getValue(), "admin");
            } else {
                log.info("Usuário admin já cadastrado no banco de dados.\nEmail: {}\nPassword: {}", emailAddress.getValue(), "admin");
            }
        };
    }
}

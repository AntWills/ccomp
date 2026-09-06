package com.ccomp.br.config.prod;

import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.persistence.UserBlaze;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.module.email.EmailAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@Profile("prod")
public class DataInitializerConfig {
    @Bean
    CommandLineRunner initUserAdmin(UserModelRepository userModelRepository,
                                    UserBlaze userBlaze,
                                    RolesServices rolesServices,
                                    PasswordEncoder encoder) {
        return args -> {
            EmailAddress emailAddress = new EmailAddress("admin@gmail.com");

            var filter = UserSearchFilter.builder()
                    .statusAccount(EnumUserStatusAccount.ACTIVE)
                    .role(EnumRoles.ADMIN)
                    .build();

            if(userBlaze.findByCursor(filter, null, 1).isEmpty()) {
                var userSaved = userModelRepository.save(UserModel.builder()
                        .name("admin")
                        .emailAddress(emailAddress)
                        .password(encoder.encode("admin"))
                        .statusAccount(EnumUserStatusAccount.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

                rolesServices.initRole(userSaved, EnumRoles.ADMIN);

                log.info("Usuário admin criado com sucesso.\nEmail: {}\nPassword: {}", emailAddress.getValue(), "admin");
            } else {
                log.info("Usuário admin já cadastrado no banco de dados.\nEmail: {}", emailAddress.getValue());
            }
        };
    }
}

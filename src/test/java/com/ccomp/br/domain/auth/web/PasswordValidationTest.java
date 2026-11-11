package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.core.dto.ResetPasswordRequestDTO;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidationTest {
    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void createValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void resetPasswordRequiresSixCharactersAndKeepsMaximum() {
        var shortPasswordRequest = new ResetPasswordRequestDTO("valid-token", "12345");
        var minimumLengthRequest = new ResetPasswordRequestDTO("valid-token", "123456");
        var overLimitRequest = new ResetPasswordRequestDTO("valid-token", "a".repeat(256));

        assertThat(validator.validate(shortPasswordRequest))
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
                    assertThat(violation.getMessage()).isEqualTo("A senha deve ter entre 6 e 255 caracteres.");
                });
        assertThat(validator.validate(minimumLengthRequest)).isEmpty();
        assertThat(validator.validate(overLimitRequest))
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
                    assertThat(violation.getMessage()).isEqualTo("A senha deve ter entre 6 e 255 caracteres.");
                });
    }

    @Test
    void registrationRequiresSixCharactersAndAcceptsExactlySix() {
        var tooShort = new RegisterUserDTO("Test User", new EmailAddress("test@example.com"), "12345");
        var minimumLength = new RegisterUserDTO("Test User", new EmailAddress("test@example.com"), "123456");

        assertThat(validator.validate(tooShort))
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
                    assertThat(violation.getMessage()).isEqualTo("A senha deve ter entre 6 e 255 caracteres.");
                });
        assertThat(validator.validate(minimumLength)).isEmpty();
    }
}

package com.ccomp.br.domain.auth.web;

import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Slf4j
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("Tentativa de criar um usuario")
    void register() throws Exception {
        log.info("===== INÍCIO TESTE: listar candidatos =====");

        RegisterUserDTO registerUserDTO = new RegisterUserDTO("Fulaninho",
                new EmailAddress("fulaninho@gmail.com"),
                "12345678");

        String requestBody = objectMapper.writeValueAsString(registerUserDTO);

        log.info("Corpo da requisição (JSON):\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(registerUserDTO));

        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        log.info("Resposta formatada:\n{}", prettyJson);

        log.info("===== FIM TESTE =====");
    }

    @Test
    void login() {
    }
}
package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.AccessTokenResponse;
import com.ccomp.br.domain.auth.persistence.RefreshTokenRepository;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Autowired
    private UserModelRepository userModelRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

//    private static final String BASE_URL = "/api/auth";
    private static final String TEST_NAME = "Fulaninho Teste";
    private static final EmailAddress TEST_EMAIL = new EmailAddress("refreshlogout@test.com");
    private static final String TEST_PASSWORD = "SenhaForte123!";

    private AccessTokenResponse createUserAndLogin() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO(TEST_NAME, TEST_EMAIL, TEST_PASSWORD);
        String registerJson = objectMapper.writeValueAsString(registerDTO);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);
        String loginJson = objectMapper.writeValueAsString(loginDTO);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        return objectMapper.readValue(loginResult.getResponse().getContentAsString(), AccessTokenResponse.class);
    }

    @Test
    @WithMockUser
    @DisplayName("Tentativa de criar um usuario")
    void signUp() throws Exception {
        log.info("===== INÍCIO TESTE: criar novo usuario =====");

        RegisterUserDTO registerUserDTO = new RegisterUserDTO("Fulaninho",
                new EmailAddress("fulaninho@gmail.com"),
                "12345678");

        String requestBody = objectMapper.writeValueAsString(registerUserDTO);

        log.info("Corpo da requisição (JSON):\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(registerUserDTO));

        var result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        log.info("Resposta formatada:\n{}", prettyJson);

        endTest();
    }

    @Test
    @WithMockUser
    @DisplayName("Tentativa de login")
    void sigIn() throws Exception {
        log.info("===== INÍCIO TESTE: login =====");
        EmailAddress email = new EmailAddress("fulaninho@gmail.com");
        String password = "12345678";

        RegisterUserDTO registerUserDTO = new RegisterUserDTO("Fulaninho", email, password);
        createUser(registerUserDTO);

        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(email, password);
        String requestBody = objectMapper.writeValueAsString(loginRequestDTO);


        var result = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        printResult(result);

        var user = userModelRepository.findByEmailAddress(email).get();
        var refresh = refreshTokenRepository.findByUserId(user.getId()).get();

        log.info("Dados user: \n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(user));
        log.info("Refresh user: \n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(refresh));

        endTest();
    }

    private void createUser(RegisterUserDTO dto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(dto);

        var result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    @Test
    @DisplayName("Deve renovar o access token com refresh token válido")
    void shouldRefreshTokenSuccessfully() throws Exception {
        log.info("===== INÍCIO TESTE: refresh token válido =====");

        AccessTokenResponse initialLogin = createUserAndLogin();
        String originalRefreshToken = initialLogin.refreshToken();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(originalRefreshToken);
        String refreshJson = objectMapper.writeValueAsString(refreshRequest);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andReturn();

        RefreshTokenResponse response = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                RefreshTokenResponse.class
        );

        assertNotNull(response.accessToken(), "Access token não pode ser nulo");

        log.info("Novo access token: {}", response.accessToken());
        printResult(getAndPrintUserDataForAccessToken(response.accessToken()));

        endTest();
    }

    @Test
    @DisplayName("Deve invalidar refresh token ao fazer logout")
    void shouldLogoutAndInvalidateRefreshToken() throws Exception {
        log.info("===== INÍCIO TESTE: logout e invalidação de refresh =====");

        // 1. Cria usuário e faz login
        AccessTokenResponse loginResponse = createUserAndLogin();
        String refreshToken = loginResponse.refreshToken();

        // Verifica que o refresh existe no banco
        var user = userModelRepository.findByEmailAddress(TEST_EMAIL).orElseThrow();
        assertTrue(refreshTokenRepository.findByUserId(user.getId()).isPresent());

        // 2. Faz logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);
        String logoutJson = objectMapper.writeValueAsString(logoutRequest);

        log.info("===== RELIZANDO LOGOUT =====");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutJson))
                .andExpect(status().isNoContent());  // ou .isNotFound() se ainda não implementou

        // 3. Tenta usar o mesmo refresh token novamente → deve falhar
        RefreshTokenRequest retryRefresh = new RefreshTokenRequest(refreshToken);
        String retryJson = objectMapper.writeValueAsString(retryRefresh);

        log.info("===== TENTANDO FAZER REFRESH =====");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retryJson))
                .andExpect(status().isUnauthorized());

        // 4. Verifica no banco que o refresh foi removido/invalidado
        assertFalse(refreshTokenRepository.findByUserId(user.getId()).isPresent(),
                "O refresh token deveria ter sido removido/invalidado após logout");

        endTest();
    }

    private void printResult(MvcResult result) throws UnsupportedEncodingException {
        String jsonResponse = result.getResponse().getContentAsString();
        Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        log.info("Resposta formatada:\n{}", prettyJson);
    }

    private MvcResult getAndPrintUserDataForAccessToken(String token) throws Exception {
        return mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    private void endTest(){
        log.info("===== FIM TESTE =====");
    }
}
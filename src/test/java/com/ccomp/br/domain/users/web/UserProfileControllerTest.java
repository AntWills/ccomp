package com.ccomp.br.domain.users.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Slf4j
class UserProfileControllerTest {
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    PasswordEncoder passwordEncoder;
//
//    @Autowired
//    private UserModelRepository userModelRepository;
//
//    private UserModel savedUser;
//
//    private String userPassword = "12345678";
//
//    @BeforeEach
//    void setup(){
//        savedUser = userModelRepository.save(
//                UserModel.builder()
//                        .name("Fulaninho")
//                        .emailAddress(new EmailAddress("fulaninho@gmail.com"))
//                        .password(passwordEncoder.encode(userPassword)).build()
//        );
//
//        log.info("Usuário de teste criado: ID = {}, email = {}",
//                savedUser.getId(), savedUser.getEmailAddress().getValue());
//    }
//
//    @Test
//    void getAll() {
//    }
//
//    @Test
//    @WithMockUser
//    @DisplayName("Fazer login e recuperar dados do usuario")
//    void getMe() throws Exception {
//        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(
//                savedUser.getEmailAddress(), userPassword
//        );
//
//        String requestBody = objectMapper.writeValueAsString(loginRequestDTO);
//
//        log.info("Corpo da requisição (JSON):\n{}",
//                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(loginRequestDTO));
//
//        var resultLogin = mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andReturn();
//
//        String jsonResponse = resultLogin.getResponse().getContentAsString();
//        Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
//        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
//
//        log.info("Resposta do Login:\n{}", prettyJson);
//
//        String accessToken = objectMapper.readTree(jsonResponse).path("accessToken").asString();
//
//        var resultMe = mockMvc.perform(get("/api/users/me")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andReturn();
//
//        String jsonResponse2 = resultMe.getResponse().getContentAsString();
//        Object jsonObject2 = objectMapper.readValue(jsonResponse2, Object.class);
//        String prettyJson2 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject2);
//
//        log.info("Resposta de details:\n{}", prettyJson2);
//
//        log.info("===== FIM TESTE =====");
//    }
}
package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.auth.application.JwtService;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.persistence.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
//@WithMockUser
@Slf4j
class EventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JwtService jwtService;

    private String jwt;

    @BeforeEach
    void setUp(){
        UUID userId = UUID.fromString("18b31f15-e2e8-4a8a-a1fc-4dc17d131ef1");

        jwt = jwtService.getAccessToken(userId);
    }

        @Test
        @DisplayName("Fazer login e recuperar dados do usuario")
        @Sql(scripts = {
                "/sql/insert-users-test.sql"
        }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void create() throws Exception {
        log.info("===== INÍCIO TESTE: criar evento =====");

        var eventDto = new CreateEventRequestDTO("Evento Teste 1", null, null);

        String requestBody = objectMapper.writeValueAsString(eventDto);

        log.info("Corpo da requisição (JSON):\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventDto));

        var result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        log.info("===== FIM TESTE =====");
    }

    @Test
    @DisplayName("Buscar um evento pelo id")
    @Sql(scripts = {
            "/sql/insert-users-test.sql",
            "/sql/insert-events-test.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getById() throws Exception {
        log.info("===== INÍCIO TESTE: Buscar evento =====");

        var result = mockMvc.perform(get("/api/events/40")
                        .contentType(MediaType.APPLICATION_JSON)
//                        .header("Authorization", "Bearer " + jwt)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        log.info("===== FIM TESTE =====");
    }
}
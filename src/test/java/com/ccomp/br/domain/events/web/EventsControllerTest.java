package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.security.jwt.application.JwtService;
import com.ccomp.br.domain.events.dto.CreateActivityRequest;
import com.ccomp.br.domain.events.dto.CreateEventRequestDTO;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.EventRepository;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.editors.EventEditorRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
//@WithMockUser
@Slf4j
class EventsControllerTest {
    private Long EVENT_ID = 1L;
    private UUID OWNER_ID = UUID.fromString("18b31f15-e2e8-4a8a-a1fc-4dc17d131ef1"); // Cleber

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventEditorRepository editorRepository;

    @Autowired
    private EventActivityRepository activityRepository;

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

        endTest();
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

        endTest();
    }

    @Test
    @DisplayName("Adicionar Editor ao evento.")
    @Sql(scripts = {
            "/sql/insert-users-test.sql",
            "/sql/insert-events-test.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void addEditor() throws Exception {
        log.info("===== INÍCIO TESTE: Adicionar Editor =====");

        String editorId = "e5a1b9d5-7f4e-405e-b5d1-5e6f7a8b9c05"; // Fernanda
        String jwt = jwtService.getAccessToken(OWNER_ID);

        var result = mockMvc.perform(post("/api/events/{eventId}/editors/{userId}", EVENT_ID, editorId)
                                .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        endTest();
    }

    @Test
    @DisplayName("Remover editor do evento.")
    @Sql(scripts = {
            "/sql/insert-users-test.sql",
            "/sql/insert-events-test.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void removeEditor() throws Exception {
        log.info("===== INÍCIO TESTE: Remover Editor =====");

        String editorId = "e5a1b9d5-7f4e-405e-b5d1-5e6f7a8b9c05"; // Fernanda
        String jwt = jwtService.getAccessToken(OWNER_ID);
        Event event = eventRepository.findById(EVENT_ID).get();

        EventEditor editor = EventEditor.builder()
                .event(event)
                .userId(UUID.fromString(editorId))
                .assignedAt(LocalDateTime.now())
                .build();
        editorRepository.save(editor);

        var result = mockMvc.perform(delete("/api/events/{eventId}/editors/{userId}", EVENT_ID, editorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        endTest();
    }

    @Test
    @DisplayName("Adicionar atividade ao evento.")
    @Sql(scripts = {
            "/sql/insert-users-test.sql",
            "/sql/insert-events-test.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void addActivity() throws Exception {
        log.info("===== INÍCIO TESTE: Adicionar Atividade =====");

        String jwt = jwtService.getAccessToken(OWNER_ID);

        CreateActivityRequest requestDto = new CreateActivityRequest("Minha atividade1", "Com ou sem descrição?");
        String requestJson = objectMapper.writeValueAsString(requestDto);


        var result = mockMvc.perform(post("/api/events/{eventId}/activities", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        endTest();
    }

    @Test
    @DisplayName("Remover atividade do evento.")
    @Sql(scripts = {
            "/sql/insert-users-test.sql",
            "/sql/insert-events-test.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void removeActivity() throws Exception {
        log.info("===== INÍCIO TESTE: Remover Atividade =====");

        String jwt = jwtService.getAccessToken(OWNER_ID);

        Event event = eventRepository.findById(EVENT_ID).get();

        EventActivity activity = activityRepository.save(EventActivity.builder()
                        .title("Minha atividade1")
                        .description("Com ou sem descrição?")
                        .event(event)
                .build());


        var result = mockMvc.perform(delete("/api/events/activities/{id}", activity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(result1 -> {
                    String jsonResponse = result1.getResponse().getContentAsString();
                    Object jsonObject = objectMapper.readValue(jsonResponse, Object.class);
                    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                    log.info("Resposta formatada:\n{}", prettyJson);
                });

        endTest();
    }

    private void endTest(){
        log.info("===== FIM TESTE =====");
    }
}
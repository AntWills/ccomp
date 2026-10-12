package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class CertificateVariablesServices {
    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    private final UserManagement userManagement;
    private final EventCache eventCache;
    private final EventActivityRepository activityRepository;

    public static final List<String> VARIABLES = List.of(
            "{participante.nome}",
            "{evento.title}",
            "{evento.local}",
            "{evento.carga_horaria}",
            "{atividade.nome}",
            "{atividade.local}",
            "{atividade.carga_horaria}",
            "{eveto.organizacao}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public CertificateVariablesServices(EnrollmentActivityDslRepository enrollmentActivityDslRepository,
                                        UserManagement userManagement,
                                        EventCache eventCache,
                                        EventActivityRepository activityRepository) {
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.userManagement = userManagement;
        this.eventCache = eventCache;
        this.activityRepository = activityRepository;
    }

    public Map<String, String> loadEventVariablesValues(List<String> variables, Long eventId, UUID userId) {
        return Map.of(
                "{participante.nome}", "Nome de Exemplo",
                "{evento.title}", "Evento de Teste",
                "{evento.carga_horaria}", "20 horas"
        );
    }

    public Map<String, String> loadActivityVariablesValues(List<String> variables, Long activityId, UUID userId) {
        // 1. Validação prévia de Inscrição e Check-in (Síncrona - Interrompe antes de abrir threads)
        var enrollmentActivity = enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não é inscrito nesta atividade."));

        if (!enrollmentActivity.haveCheckIn()) {
            throw new DomainException("O inscrito não fez o check-in nesta atividade.");
        }

        // 2. Identifica quais dados realmente precisam ser buscados (Evita I/O desnecessário)
        boolean needsUser = variables == null || variables.contains("{participante.nome}");
        boolean needsActivity = variables == null || variables.stream().anyMatch(v -> v.startsWith("{atividade."));
        boolean needsEvent = variables == null || variables.stream().anyMatch(v -> v.startsWith("{evento."));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Fork-1: Busca do Usuário
            CompletableFuture<UserDTO> userFuture = needsUser
                    ? CompletableFuture.supplyAsync(() -> userManagement.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado.")), executor)
                    : CompletableFuture.completedFuture(null);

            // Fork-2: Busca da Atividade
            CompletableFuture<EventActivity> activityFuture = (needsActivity || needsEvent)
                    ? CompletableFuture.supplyAsync(() -> activityRepository.findById(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada.")), executor)
                    : CompletableFuture.completedFuture(null);

            // Fork-3: Busca do Evento (Depende do id retornado na Atividade)
            CompletableFuture<EventDTO> eventFuture = activityFuture.thenApplyAsync(activity -> {
                if (activity == null || !needsEvent) return null;
                return eventCache.findById(activity.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Evento associado à atividade não encontrado."));
            }, executor);

            CompletableFuture.allOf(userFuture, activityFuture, eventFuture).join();

            Map<String, String> values = new HashMap<>();

            extractValuesFromUser(values, userFuture.join());
            extractValuesFromActivity(values, activityFuture.join());
            extractValuesFromEvent(values, eventFuture.join());

            return values;
        }
    }

    private void extractValuesFromUser(Map<String, String> values, @Nullable UserDTO user) {
        if (user != null) {
            values.put("{participante.nome}", user.name());
        }
    }

    private void extractValuesFromActivity(Map<String, String> values, @Nullable EventActivity activity) {
        if (activity != null) {
            values.put("{atividade.nome}", activity.getTitle());
            values.put("{atividade.carga_horaria}", activity.getWorkloadHours() != null ? activity.getWorkloadHours() + " horas" : "");
            values.put("{atividade.local}", activity.getLocation() != null ? activity.getLocation() : "");
        }
    }

    private void extractValuesFromEvent(Map<String, String> values, @Nullable EventDTO event) {
        if (event != null) {
            values.put("{evento.title}", event.title());
//                values.put("{evento.carga_horaria}", event.workloadHours() != null ? event.workloadHours() + " horas" : "");
//                values.put("{evento.local}", event. != null ? event.location() : "");
        }
    }
}
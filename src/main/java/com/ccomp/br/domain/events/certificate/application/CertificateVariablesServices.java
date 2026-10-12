package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Service
public class CertificateVariablesServices {
    @Value("${app.frontend.validate-certificate-url}")
    String validationBaseUrl;

    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    private final EventActivityDslRepository activityDslRepository;
    private final UserManagement userManagement;
    private final EventCache eventCache;
    private final CertificateValidatorService validatorService;
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
                                        EventActivityDslRepository activityDslRepository,
                                        UserManagement userManagement,
                                        EventCache eventCache, CertificateValidatorService validatorService,
                                        EventActivityRepository activityRepository) {
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.activityDslRepository = activityDslRepository;
        this.userManagement = userManagement;
        this.eventCache = eventCache;
        this.validatorService = validatorService;
        this.activityRepository = activityRepository;
    }

    public Map<String, String> loadEventVariablesValues(List<String> variables, Long eventId, UUID userId) {
        return Map.of(
                "{participante.nome}", "Nome de Exemplo",
                "{evento.title}", "Evento de Teste",
                "{evento.carga_horaria}", "20 horas"
        );
    }

    public Map<String, String> loadActivityVariablesValues(@NonNull List<String> variables, Long activityId, UUID userId) {
        // 1. Validação prévia de Inscrição e Check-in (Síncrona - Interrompe antes de abrir threads)
        var enrollmentActivity = enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não é inscrito nesta atividade."));

        if (!enrollmentActivity.haveCheckIn()) {
            throw new DomainException("O inscrito não fez o check-in nesta atividade.");
        }

        // 2. Identifica quais dados realmente precisam ser buscados (Evita I/O desnecessário)
        boolean needsUser = variables.contains("{participante.nome}");
        boolean needsActivity =  variables.stream().anyMatch(v -> v.startsWith("{atividade."));
        boolean needsEvent = variables.stream().anyMatch(v -> v.startsWith("{evento."));
        boolean needsCode = variables.contains("{validacao.codigo}")
                || variables.contains("{validacao.url}");

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

            CompletableFuture<EventCertificateValidationCode> validationCodeFuture = activityFuture
                    .thenApplyAsync(activity -> {
                        if(activity == null || !needsCode) return null;

                        return validatorService.issueForActivity(activity.getEventId(), activity.getId(), userId);
                    }, executor);

            // Fork-4: Busca do Evento (Depende do id retornado na Atividade)
            CompletableFuture<EventDTO> eventFuture = activityFuture.thenApplyAsync(activity -> {
                if (activity == null || !needsEvent) return null;
                return eventCache.findById(activity.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Evento associado à atividade não encontrado."));
            }, executor);



            CompletableFuture.allOf(userFuture, activityFuture, eventFuture, validationCodeFuture).join();

            Map<String, String> values = new HashMap<>();

            extractValuesFromUser(values, userFuture.join());
            extractValuesFromActivity(values, activityFuture.join());
            extractValuesFromEvent(values, userId, eventFuture.join());
            extractValuesFromValidator(values, validationCodeFuture.join());

            return values;
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao carregar variaveis do certificado.", e.getCause());
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
            values.put("{atividade.carga_horaria}", activity.getWorkloadHours() != null ? activity.getWorkloadHours() + " horas" : "Não consta");
            values.put("{atividade.local}", activity.getLocation() != null ? activity.getLocation() : "Não consta");
        }
    }

    private void extractValuesFromEvent(Map<String, String> values, UUID userId, @Nullable EventDTO event) {
        if (event != null) {
            values.put("{evento.title}", event.title());
            values.put("{evento.carga_horaria}", activityDslRepository.sumWorkloadHoursByCheckIn(userId, event.id()).toString());
        }
    }

    private void extractValuesFromValidator(Map<String, String> values, EventCertificateValidationCode validationCode) {
        if(validationCode != null) {
            values.put("{validacao.codigo}", validationCode.getCode());
            values.put("{validacao.url}", validationBaseUrl);
        }
    }
}
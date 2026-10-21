package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final ActivityCache activityCache;
    private final EnrollmentRepository enrollmentRepository;
    private final EventRepository eventRepository;

    public static final List<String> VARIABLES = List.of(
            "{participante.nome}",
            "{evento.title}",
            "{evento.local}",
            "{evento.carga_horaria}",
            "{atividade.nome}",
            "{atividade.local}",
            "{atividade.carga_horaria}",
            "{evento.organizacao}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public CertificateVariablesServices(EnrollmentActivityDslRepository enrollmentActivityDslRepository,
                                        EventActivityDslRepository activityDslRepository,
                                        UserManagement userManagement,
                                        EventCache eventCache, CertificateValidatorService validatorService,
                                        ActivityCache activityCache,
                                        EnrollmentRepository enrollmentRepository,
                                        EventRepository eventRepository) {
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.activityDslRepository = activityDslRepository;
        this.userManagement = userManagement;
        this.eventCache = eventCache;
        this.validatorService = validatorService;
        this.activityCache = activityCache;
        this.enrollmentRepository = enrollmentRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Carrega os valores reais das variáveis do certificado do evento. Exige que o
     * participante esteja inscrito no evento com o credenciamento (check-in) realizado,
     * e soma a carga horária das atividades em que ele realmente participou.
     */
    public Map<String, String> loadEventVariablesValues(List<String> variables, Long eventId, UUID userId) {
        if (userId == null)
            throw new AccessDeniedException("É necessário estar autenticado para gerar o certificado.");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não é inscrito no evento."));

        if (!enrollment.haveCheckIn())
            throw new DomainException("O credenciamento (check-in) no evento não foi realizado.");

        boolean needsUser = variables == null || variables.contains("{participante.nome}");
        boolean needsWorkload = variables == null || variables.contains("{evento.carga_horaria}");
        boolean needsCode = variables == null || variables.contains("{validacao.codigo}")
                || variables.contains("{validacao.url}");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<UserDTO> userFuture = needsUser
                    ? CompletableFuture.supplyAsync(() -> userManagement.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado.")), executor)
                    : CompletableFuture.completedFuture(null);

            // Fork-2: Soma da carga horária das atividades em que o participante fez check-in
            CompletableFuture<BigDecimal> workloadFuture = needsWorkload
                    ? CompletableFuture.supplyAsync(
                            () -> activityDslRepository.sumWorkloadHoursByCheckIn(userId, eventId), executor)
                    : CompletableFuture.completedFuture(null);

            // Fork-3: Código de validação (emitido uma única vez por participante/evento)
            CompletableFuture<EventCertificateValidationCode> validationCodeFuture = needsCode
                    ? CompletableFuture.supplyAsync(
                            () -> validatorService.issueForEvent(eventId, userId), executor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture.allOf(userFuture, workloadFuture, validationCodeFuture).join();

            Map<String, String> values = new HashMap<>();

            extractValuesFromUser(values, userFuture.join());
            values.put("{evento.title}", event.getTitle());
            values.put("{evento.local}", event.getAddress() != null
                    ? event.getAddress()
                    : "Não consta");
            if (workloadFuture.join() != null) {
                values.put("{evento.carga_horaria}", workloadFuture.join().stripTrailingZeros().toPlainString());
            }
            extractValuesFromValidator(values, validationCodeFuture.join());

            return values;
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Falha ao carregar variaveis do certificado.", e.getCause());
        }
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
            CompletableFuture<ActivityDTO> activityFuture = (needsActivity || needsEvent)
                    ? CompletableFuture.supplyAsync(() -> activityCache.findById(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada.")), executor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture<EventCertificateValidationCode> validationCodeFuture = activityFuture
                    .thenApplyAsync(activity -> {
                        if(activity == null || !needsCode) return null;

                        return validatorService.issueForActivity(activity.eventId(), activity.id(), userId);
                    }, executor);

            // Fork-4: Busca do Evento (Depende do id retornado na Atividade)
            CompletableFuture<EventDTO> eventFuture = activityFuture.thenApplyAsync(activity -> {
                if (activity == null || !needsEvent) return null;
                return eventCache.findById(activity.eventId())
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

    private void extractValuesFromActivity(Map<String, String> values, @Nullable ActivityDTO activity) {
        if (activity != null) {
            values.put("{atividade.nome}", activity.title());
            values.put("{atividade.carga_horaria}", activity.workloadHours() != null ? activity.workloadHours() + " horas" : "Não consta");
            values.put("{atividade.local}", activity.location() != null ? activity.location() : "Não consta");
        }
    }

    private void extractValuesFromEvent(Map<String, String> values, UUID userId, @Nullable EventDTO event) {
        if (event != null) {
            values.put("{evento.title}", event.title());
            values.put("{evento.local}", event.address() != null ? event.address() : "Não consta");
            values.put("{evento.carga_horaria}",
                    activityDslRepository.sumWorkloadHoursByCheckIn(userId, event.id()).stripTrailingZeros().toPlainString());
        }
    }

    private void extractValuesFromValidator(Map<String, String> values, @Nullable EventCertificateValidationCode validationCode) {
        if(validationCode != null) {
            values.put("{validacao.codigo}", validationCode.getCode());
            values.put("{validacao.url}", buildValidationUrl(validationCode.getCode()));
        }
    }

    /** Monta a URL pública de validação a partir da base configurada + código. */
    private String buildValidationUrl(String code) {
        return validationBaseUrl.replaceAll("/+$", "") + "/" + code;
    }

}

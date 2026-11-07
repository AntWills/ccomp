package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
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
     * participante esteja inscrito e com status CHECKED_IN no evento, e soma a carga
     * horária das atividades em que ele realmente participou.
     */
    public Map<String, String> loadEventVariablesValues(List<String> variables, Long eventId, UUID userId) {
        if (userId == null)
            throw new AccessDeniedException("É necessário estar autenticado para gerar o certificado.");

        Event event = findCertificateEligibleEvent(eventId);

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não é inscrito no evento."));

        if (!enrollment.haveCheckIn())
            throw new DomainException("O participante não fez check-in no evento.");

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
                            () -> addHours(event.getWorkloadHours(),
                                    activityDslRepository.sumWorkloadHoursByCheckIn(userId, eventId)), executor)
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

    public Map<String, String> loadInstructorVariablesValues(List<String> variables, Long eventId,
                                                            Long instructorId, UUID userId) {
        Event event = findCertificateEligibleEvent(eventId);
        if (activityDslRepository.findConfirmedInstructorAccount(instructorId, eventId)
                .filter(userId::equals).isEmpty()) {
            throw new AccessDeniedException("O usuário não possui vínculo confirmado como ministrante neste evento.");
        }
        return loadRoleEventVariables(variables, event, userId,
                activityDslRepository.sumWorkloadHoursByInstructor(userId, eventId), EnumCertificateType.INSTRUCTOR);
    }

    public UUID findInstructorCertificateRecipient(Long eventId, Long instructorId) {
        return activityDslRepository.findConfirmedInstructorAccount(instructorId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ministrante confirmado com conta vinculada não encontrado neste evento."));
    }

    public Map<String, String> loadEditorVariablesValues(List<String> variables, Long eventId, UUID userId) {
        Event event = findCertificateEligibleEvent(eventId);
        if (event.getTeamCertificateWorkloadHours() == null) {
            throw new DomainException("A carga horária dos certificados da equipe não foi definida para este evento.");
        }
        return loadRoleEventVariables(variables, event, userId,
                event.getTeamCertificateWorkloadHours(), EnumCertificateType.EDITOR);
    }

    private Event findCertificateEligibleEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (event.getExecutionStatus() != EnumEventExecutionStatus.FINISHED
                || event.getStatus() == EnumEventStatus.CANCELED) {
            throw new DomainException("O certificado do evento só pode ser emitido após seu encerramento.");
        }
        return event;
    }

    private Map<String, String> loadRoleEventVariables(List<String> variables, Event event, UUID userId,
                                                        BigDecimal workload, EnumCertificateType type) {
        UserDTO user = userManagement.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        boolean needsCode = variables == null || variables.contains("{validacao.codigo}")
                || variables.contains("{validacao.url}");
        EventCertificateValidationCode code = needsCode
                ? validatorService.issueForEvent(event.getId(), userId, type)
                : null;
        Map<String, String> values = new HashMap<>();
        values.put("{participante.nome}", user.name());
        values.put("{evento.title}", event.getTitle());
        values.put("{evento.local}", event.getAddress() == null ? "Não consta" : event.getAddress());
        values.put("{evento.carga_horaria}", workload.stripTrailingZeros().toPlainString());
        extractValuesFromValidator(values, code);
        return values;
    }

    private BigDecimal addHours(BigDecimal first, BigDecimal second) {
        return (first == null ? BigDecimal.ZERO : first).add(second == null ? BigDecimal.ZERO : second);
    }

    public Map<String, String> loadActivityVariablesValues(@NonNull List<String> variables, Long activityId, UUID userId) {
        // 1. Validação prévia de Inscrição e Check-in (Síncrona - Interrompe antes de abrir threads)
        var enrollmentActivity = enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não é inscrito nesta atividade."));

        if (!enrollmentActivity.getEnrollment().haveCheckIn()) {
            throw new DomainException("O participante não fez check-in no evento.");
        }

        if (!enrollmentActivity.haveCheckIn()) {
            throw new DomainException("O inscrito não participou desta atividade.");
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
            values.put("{evento.carga_horaria}", addHours(event.workloadHours(),
                    activityDslRepository.sumWorkloadHoursByCheckIn(userId, event.id()))
                    .stripTrailingZeros().toPlainString());
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

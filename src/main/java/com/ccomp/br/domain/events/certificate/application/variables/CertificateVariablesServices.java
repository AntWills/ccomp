package com.ccomp.br.domain.events.certificate.application.variables;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.certificate.application.CertificateValidatorService;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateType;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateVariableContext;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

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

    public static final List<String> PARTICIPANT_VARIABLES = List.of(
            "{participante.nome}",
            "{evento.carga_horaria}",
            "{evento.title}",
            "{evento.local}",
            "{evento.organizacao}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public static final List<String> ACTIVITY_VARIABLES = List.of(
            "{participante.nome}",
            "{evento.title}",
            "{evento.local}",
            "{evento.organizacao}",
            "{evento.carga_horaria}",
            "{atividade.nome}",
            "{atividade.local}",
            "{atividade.carga_horaria}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public static final List<String> INSTRUCTOR_VARIABLES = List.of(
            "{ministrante.nome}",
            "{ministrante.carga_horaria}",
            "{evento.title}",
            "{evento.local}",
            "{evento.organizacao}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public static final List<String> EDITOR_VARIABLES = List.of(
            "{equipe.nome}",
            "{equipe.carga_horaria}",
            "{evento.title}",
            "{evento.local}",
            "{evento.organizacao}",
            "{validacao.codigo}",
            "{validacao.url}"
    );

    public static List<String> variablesFor(EnumCertificateVariableContext context) {
        return switch (context) {
            case PARTICIPANT -> PARTICIPANT_VARIABLES;
            case ACTIVITY -> ACTIVITY_VARIABLES;
            case INSTRUCTOR -> Stream.concat(INSTRUCTOR_VARIABLES.stream(),
                    List.of("{participante.nome}", "{evento.carga_horaria}").stream()).toList();
            case EDITOR -> Stream.concat(EDITOR_VARIABLES.stream(),
                    List.of("{participante.nome}", "{evento.carga_horaria}").stream()).toList();
        };
    }

    static List<String> acceptedVariablesFor(EnumCertificateVariableContext context) {
        return variablesFor(context);
    }

    public CertificateVariablesServices(EnrollmentActivityDslRepository enrollmentActivityDslRepository,
                                        EventActivityDslRepository activityDslRepository,
                                        UserManagement userManagement,
                                        EventCache eventCache, CertificateValidatorService validatorService,
                                        ActivityCache activityCache,
                                        EnrollmentRepository enrollmentRepository) {
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.activityDslRepository = activityDslRepository;
        this.userManagement = userManagement;
        this.eventCache = eventCache;
        this.validatorService = validatorService;
        this.activityCache = activityCache;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Carrega os valores reais das variáveis do certificado do evento. Exige que o
     * participante esteja inscrito e com status CHECKED_IN no evento, e soma a carga
     * horária das atividades em que ele realmente participou.
     */
    public Map<String, String> loadEventVariablesValues(List<String> variables, Long eventId, UUID userId) {
        EventDTO event = findCertificateEligibleEventDto(eventId);

        Enrollment enrollment = enrollmentRepository.findByUserIdAndEventId(userId, eventId)
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
                            () -> addHours(event.workloadHours(),
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
            values.put("{evento.title}", event.title());
            values.put("{evento.local}", event.address() != null
                    ? event.address()
                    : "Não consta");
            values.put("{evento.organizacao}", event.organization() != null
                    ? event.organization() : "Não consta");
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
        EventDTO event = findCertificateEligibleEventDto(eventId);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Optional<UUID>> instructorAccountFuture = CompletableFuture.supplyAsync(
                    () -> activityDslRepository.findConfirmedInstructorAccount(instructorId, eventId), executor);
            CompletableFuture<BigDecimal> workloadFuture = CompletableFuture.supplyAsync(
                    () -> activityDslRepository.sumWorkloadHoursByInstructor(userId, eventId), executor);
            CompletableFuture.allOf(instructorAccountFuture, workloadFuture).join();
            if (instructorAccountFuture.join().filter(userId::equals).isEmpty()) {
                throw new AccessDeniedException("O usuário não possui vínculo confirmado como ministrante neste evento.");
            }
            return loadRoleEventVariables(variables, event.id(), event.title(), event.address(), event.organization(),
                    userId, workloadFuture.join(), EnumCertificateType.INSTRUCTOR);
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Falha ao carregar variáveis do certificado.", e.getCause());
        }
    }

    public Map<String, String> loadGuestInstructorVariablesValues(Long eventId, UUID userId) {
        EventDTO event = findCertificateEligibleEventDto(eventId);
        return loadRoleEventVariables(INSTRUCTOR_VARIABLES, eventId, event.title(), event.address(),
                event.organization(), userId, activityDslRepository.sumWorkloadHoursByInstructor(userId, eventId),
                EnumCertificateType.INSTRUCTOR);
    }

    public UUID findInstructorCertificateRecipient(Long eventId, Long instructorId) {
        return activityDslRepository.findConfirmedInstructorAccount(instructorId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ministrante confirmado com conta vinculada não encontrado neste evento."));
    }

    public BigDecimal sumInstructorWorkload(UUID userId, Long eventId) {
        return activityDslRepository.sumWorkloadHoursByInstructor(userId, eventId);
    }

    public Map<String, String> loadManualInstructorVariablesValues(Long eventId, String instructorName,
                                                                  BigDecimal workloadHours) {
        EventDTO event = findCertificateEligibleEventDto(eventId);
        String formattedWorkload = workloadHours == null
                ? "0" : workloadHours.stripTrailingZeros().toPlainString();
        Map<String, String> values = new HashMap<>();
        values.put("{ministrante.nome}", instructorName);
        values.put("{ministrante.carga_horaria}", formattedWorkload);
        values.put("{participante.nome}", instructorName);
        values.put("{evento.title}", event.title());
        values.put("{evento.local}", event.address() == null ? "Não consta" : event.address());
        values.put("{evento.organizacao}", event.organization() == null ? "Não consta" : event.organization());
        values.put("{evento.carga_horaria}", formattedWorkload);
        values.put("{validacao.codigo}", "Emissão manual");
        values.put("{validacao.url}", "");
        return values;
    }

    public Map<String, String> loadEditorVariablesValues(List<String> variables, Long eventId, UUID userId) {
        EventDTO event = findCertificateEligibleEventDto(eventId);
        if (event.teamCertificateWorkloadHours() == null)
            throw new DomainException("A carga horária dos certificados da equipe não foi definida para este evento.");

        return loadRoleEventVariables(variables, event.id(), event.title(), event.address(), event.organization(),
                userId, event.teamCertificateWorkloadHours(), EnumCertificateType.EDITOR);
    }

    private EventDTO findCertificateEligibleEventDto(Long eventId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (event.executionStatus() != EnumEventExecutionStatus.FINISHED
                || event.status() == EnumEventStatus.CANCELED) {
            throw new DomainException("O certificado do evento só pode ser emitido após seu encerramento.");
        }
        return event;
    }

    private Map<String, String> loadRoleEventVariables(List<String> variables, Long eventId, String eventTitle,
                                                        String eventAddress, String organization, UUID userId,
                                                        BigDecimal workload, EnumCertificateType type) {
        boolean needsCode = variables == null || variables.contains("{validacao.codigo}")
                || variables.contains("{validacao.url}");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<UserDTO> userFuture = CompletableFuture.supplyAsync(() -> userManagement.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado.")), executor);
            CompletableFuture<EventCertificateValidationCode> codeFuture = needsCode
                    ? CompletableFuture.supplyAsync(() -> validatorService.issueForEvent(eventId, userId, type), executor)
                    : CompletableFuture.completedFuture(null);

            CompletableFuture.allOf(userFuture, codeFuture).join();
            UserDTO user = userFuture.join();
            String formattedWorkload = workload.stripTrailingZeros().toPlainString();

            Map<String, String> values = new HashMap<>();

            values.put("{participante.nome}", user.name());
            values.put("{evento.title}", eventTitle);
            values.put("{evento.local}", eventAddress == null ? "Não consta" : eventAddress);
            values.put("{evento.carga_horaria}", formattedWorkload);
            values.put("{evento.organizacao}", organization == null ? "Não consta" : organization);

            if (type == EnumCertificateType.INSTRUCTOR) {
                values.put("{ministrante.nome}", user.name());
                values.put("{ministrante.carga_horaria}", formattedWorkload);
            } else if (type == EnumCertificateType.EDITOR) {
                values.put("{equipe.nome}", user.name());
                values.put("{equipe.carga_horaria}", formattedWorkload);
            }
            extractValuesFromValidator(values, codeFuture.join());
            return values;
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Falha ao carregar variáveis do certificado.", e.getCause());
        }
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

            CompletableFuture<BigDecimal> eventWorkloadFuture = eventFuture.thenApplyAsync(event -> {
                if (event == null || !variables.contains("{evento.carga_horaria}")) return null;
                return addHours(event.workloadHours(),
                        activityDslRepository.sumWorkloadHoursByCheckIn(userId, event.id()));
            }, executor);
            CompletableFuture.allOf(userFuture, activityFuture, eventFuture, validationCodeFuture,
                    eventWorkloadFuture).join();

            Map<String, String> values = new HashMap<>();

            extractValuesFromUser(values, userFuture.join());
            extractValuesFromActivity(values, activityFuture.join());
            extractValuesFromEvent(values, eventFuture.join(), eventWorkloadFuture.join());
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
            values.put("{atividade.carga_horaria}", activity.workloadHours() == null
                    ? "0" : activity.workloadHours().stripTrailingZeros().toPlainString());
            values.put("{atividade.local}", activity.location() != null ? activity.location() : "Não consta");
        }
    }

    private void extractValuesFromEvent(Map<String, String> values, @Nullable EventDTO event,
                                        @Nullable BigDecimal workload) {
        if (event != null) {
            values.put("{evento.title}", event.title());
            values.put("{evento.local}", event.address() != null ? event.address() : "Não consta");
            values.put("{evento.organizacao}", event.organization() != null
                    ? event.organization() : "Não consta");
            if (workload != null) {
                values.put("{evento.carga_horaria}", workload.stripTrailingZeros().toPlainString());
            }
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

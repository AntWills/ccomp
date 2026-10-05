package com.ccomp.br.domain.events.activities.application;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.activities.dto.CheckInDTO;
import com.ccomp.br.domain.events.activities.external.CheckInChannel;
import com.ccomp.br.domain.events.activities.external.CheckInMessageDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckIn;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckInCache;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckInRepository;
import com.ccomp.br.domain.events.activities.utils.CheckInMapper;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity; // Assumindo o nome da entidade
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.module.qrcode.QRCode;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {
    @Mock private CheckInRepository checkInRepository;
    @Mock private CheckInCache checkInCache;
    @Mock private CheckInMapper checkInMapper;
    @Mock private EventActivityRepository activityRepository;
    @Mock private EnrollmentActivityRepository enrollmentActivityRepository;
    @Mock private EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    @Mock private EventCache eventCache;
    @Mock private EventAccessPolicy eventAccessPolicy;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CheckInService checkInService;

    private UUID userId;
    private Long activityId;
    private Long eventId;
    private UUID presenceCode;

    private EventActivity activityMock;
    private EventDTO eventDTOMock;
    private CheckInDTO checkInDTOMock;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activityId = 1L;
        eventId = 10L;
        presenceCode = UUID.randomUUID();

        activityMock = mock(EventActivity.class);
        eventDTOMock = mock(EventDTO.class);
        checkInDTOMock = mock(CheckInDTO.class);

        // Injetando a property correspondente ao @Value
        ReflectionTestUtils.setField(checkInService, "checkInUrl", "http://frontend.com/checkin");
    }

    @Nested
    @DisplayName("Generate Code - Gerar QRCode de Check-In")
    class GenerateCode {

        @Test
        @DisplayName("Gera QRCode com sucesso aproveitando check-in que já está no cache")
        void generateCode_returnsByteArray_whenCheckInIsInCache() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.getEventId()).thenReturn(eventId);

            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTOMock));
            when(eventAccessPolicy.canEdit(eventDTOMock, userId)).thenReturn(true);

            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.activityId()).thenReturn(activityId);
            when(checkInDTOMock.code()).thenReturn(presenceCode);

            // Mock estático para evitar dependências de renderização de imagem/gráficos no teste unitário
            try (MockedStatic<QRCode> mockedQrCode = mockStatic(QRCode.class)) {
                BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                mockedQrCode.when(() -> QRCode.generateQRCodeImage(anyString())).thenReturn(dummyImage);

                byte[] response = checkInService.generateCode(activityId, userId);

                assertThat(response).isNotEmpty();
                verify(checkInRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("Gera QRCode com sucesso criando um novo check-in quando não existe no cache")
        void generateCode_returnsByteArray_whenCheckInIsNotInCache() {
            CheckIn savedCheckInEntity = mock(CheckIn.class);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.getEventId()).thenReturn(eventId);

            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTOMock));
            when(eventAccessPolicy.canEdit(eventDTOMock, userId)).thenReturn(true);

            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.empty());
            when(checkInRepository.save(any(CheckIn.class))).thenReturn(savedCheckInEntity);
            when(checkInMapper.checkInToCheckInDTO(savedCheckInEntity)).thenReturn(checkInDTOMock);

            when(checkInDTOMock.activityId()).thenReturn(activityId);
            when(checkInDTOMock.code()).thenReturn(presenceCode);

            try (MockedStatic<QRCode> mockedQrCode = mockStatic(QRCode.class)) {
                BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                mockedQrCode.when(() -> QRCode.generateQRCodeImage(anyString())).thenReturn(dummyImage);

                byte[] response = checkInService.generateCode(activityId, userId);

                assertThat(response).isNotEmpty();
                verify(checkInRepository).save(any(CheckIn.class));
            }
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException quando a atividade não é encontrada")
        void generateCode_throwsResourceNotFoundException_whenActivityDoesNotExist() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkInService.generateCode(activityId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Atividade não encontrada.");
        }

        @Test
        @DisplayName("Lança AccessDeniedException quando o usuário não tem permissão de edição do evento")
        void generateCode_throwsAccessDeniedException_whenUserHasNoEditPermission() {
            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.getEventId()).thenReturn(eventId);
            when(eventCache.findById(eventId)).thenReturn(Optional.of(eventDTOMock));

            when(eventAccessPolicy.canEdit(eventDTOMock, userId)).thenReturn(false);

            assertThatThrownBy(() -> checkInService.generateCode(activityId, userId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("O usuário não tem acesso a este recurso.");
        }
    }

    @Nested
    @DisplayName("Check-In - Registrar Presença")
    class CheckInAction {

        @Test
        @DisplayName("Realiza o check-in com sucesso, envia mensagem pro RabbitMQ e salva")
        void checkIn_processesSuccessfully_whenDataIsValid() {
            EnrollmentActivity enrollmentMock = mock(EnrollmentActivity.class);

            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.checkCode(presenceCode)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.isInProgress(any(LocalDateTime.class))).thenReturn(true);

            when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId))
                    .thenReturn(Optional.of(enrollmentMock));
            when(enrollmentMock.haveCheckIn()).thenReturn(false);

            checkInService.checkIn(activityId, userId, presenceCode);

            verify(enrollmentMock).makeAttendance();
            verify(enrollmentActivityRepository).save(enrollmentMock);
            verify(rabbitTemplate).convertAndSend(
                    anyString(),
                    anyString(),
                    any(CheckInMessageDTO.class)
            );
        }

        @Test
        @DisplayName("Retorna early sem atualizar ou enviar mensagem se o usuário já fez check-in (Idempotência)")
        void checkIn_returnsEarly_whenUserAlreadyCheckedIn() {
            EnrollmentActivity enrollmentMock = mock(EnrollmentActivity.class);

            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.checkCode(presenceCode)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.isInProgress(any(LocalDateTime.class))).thenReturn(true);

            when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId))
                    .thenReturn(Optional.of(enrollmentMock));
            when(enrollmentMock.haveCheckIn()).thenReturn(true); // O Usuário já havia registrado

            checkInService.checkIn(activityId, userId, presenceCode);

            // Confirma que não prossegue para salvamento ou envio de mensagem
            verify(enrollmentMock, never()).makeAttendance();
            verify(enrollmentActivityRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(any());
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException se o código de presença cacheado não existir")
        void checkIn_throwsResourceNotFoundException_whenCheckInNotInCache() {
            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkInService.checkIn(activityId, userId, presenceCode))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Código inválido.");
        }

        @Test
        @DisplayName("Lança DomainException se o código for inválido (não coincidir)")
        void checkIn_throwsDomainException_whenCodeIsInvalid() {
            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.checkCode(presenceCode)).thenReturn(false); // Invalida o código

            assertThatThrownBy(() -> checkInService.checkIn(activityId, userId, presenceCode))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("Código inválido");
        }

        @Test
        @DisplayName("Lança DomainException se a atividade não estiver em andamento")
        void checkIn_throwsDomainException_whenActivityIsNotInProgress() {
            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.checkCode(presenceCode)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.isInProgress(any(LocalDateTime.class))).thenReturn(false); // Fora do horário

            assertThatThrownBy(() -> checkInService.checkIn(activityId, userId, presenceCode))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("Não é possível fazer check-in.");
        }

        @Test
        @DisplayName("Lança ResourceNotFoundException se o usuário não estiver matriculado na atividade")
        void checkIn_throwsResourceNotFoundException_whenUserNotEnrolled() {
            when(checkInCache.findByActivityId(activityId)).thenReturn(Optional.of(checkInDTOMock));
            when(checkInDTOMock.checkCode(presenceCode)).thenReturn(true);

            when(activityRepository.findById(activityId)).thenReturn(Optional.of(activityMock));
            when(activityMock.isInProgress(any(LocalDateTime.class))).thenReturn(true);

            when(enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkInService.checkIn(activityId, userId, presenceCode))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("O usuário não está inscrito na atividade.");
        }
    }
}
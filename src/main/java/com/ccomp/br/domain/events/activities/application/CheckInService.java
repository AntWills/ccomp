package com.ccomp.br.domain.events.activities.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckIn;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckInCache;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckInRepository;
import com.ccomp.br.domain.events.editors.application.EventEditorPermission;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.module.qrcode.QRCode;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CheckInService {
    private final CheckInRepository checkInRepository;
    private final CheckInCache checkInCache;
    private final EventActivityRepository activityRepository;
    private final EventEditorPermission editorPermission;
    private final EnrollmentActivityRepository enrollmentActivityRepository;
    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;

    @Value("${app.frontend.check-in-url}")
    private String checkInUrl;

    public CheckInService(CheckInRepository checkInRepository,
                          CheckInCache checkInCache,
                          EventActivityRepository activityRepository,
                          EventEditorPermission editorPermission,
                          EnrollmentActivityRepository enrollmentActivityRepository,
                          EnrollmentActivityDslRepository enrollmentActivityDslRepository) {
        this.checkInRepository = checkInRepository;
        this.checkInCache = checkInCache;
        this.activityRepository = activityRepository;
        this.editorPermission = editorPermission;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
    }

    public byte[] generateCode(long activityId, UUID userId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if(!editorPermission.hasPermissionEdit(activity.getEventId(), userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        CheckIn checkIn = checkInCache.findByActivityId(activityId)
                .orElseGet(() -> checkInRepository.save(
                    CheckIn.builder()
                            .activityId(activityId)
                            .code(UUID.randomUUID())
                            .build()
                ));

        String url = String.format(checkInUrl + "?activity_id=%d&code=%s", checkIn.getActivityId(), checkIn.getCode());

        try {
            BufferedImage image = QRCode.generateQRCodeImage(url);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível gerar o QRCode.", e);
        }
    }

    public void checkIn(long activityId, UUID userId, UUID presenceCode) {
        CheckIn checkIn = checkInCache.findByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Código inválido."));

        if(!checkIn.checkCode(presenceCode))
            throw new DomainException("Código inválido");

        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não existe."));

        if(!activity.isInProgress(LocalDateTime.now()))
            throw new DomainException("Não é possível fazer check-in.");

        var enrollment = enrollmentActivityDslRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("O usuário não está inscrito na atividade."));

        enrollment.makeAttendance();
        enrollmentActivityRepository.save(enrollment);
    }
}

package com.ccomp.br.domain.events.activities.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.editors.application.EventEditorPermission;
import com.ccomp.br.module.qrcode.QRCode;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.UUID;

@Service
public class CheckInService {
    private final EventActivityRepository activityRepository;
    private final EventEditorPermission editorPermission;

    public CheckInService(EventActivityRepository activityRepository, EventEditorPermission editorPermission) {
        this.activityRepository = activityRepository;
        this.editorPermission = editorPermission;
    }

    public BufferedImage generateCode(long activityId, UUID userId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if(!editorPermission.hasPermissionEdit(activity.getEventId(), userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        String presenceCode = "xyz987";
        String url = String.format("https://app.seuevento.com.br/checkin?activity_id=%d&code=%s", activityId, presenceCode);

        try {
            return QRCode.generateQRCodeImage(url);
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível gerar o QRCode.", e);
        }
    }
}

package com.ccomp.br.shared.exceptions;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;

import java.util.List;

public class SchedulingConflictException extends ConflictException {
    private final List<ActivityDTO> conflicts;

    public SchedulingConflictException(List<ActivityDTO> conflicts) {
        super("Você já está inscrito em atividades com conflito de horário.");
        this.conflicts = List.copyOf(conflicts);
    }

    @Override
    public ErrorResponse toErrorResponse(String path) {
        ErrorResponse response = super.toErrorResponse(path);
        response.setConflicts(conflicts);
        return response;
    }
}

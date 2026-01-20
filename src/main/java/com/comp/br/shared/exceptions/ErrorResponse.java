package com.comp.br.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String route;
    private List<String> mensages;
    private Map<String, String> details; // Erros por campo

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String route,
                         List<String> mensages, Map<String, String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.route = route;
        this.mensages = mensages;
        this.details = details != null ? details : new HashMap<>();
    }
}

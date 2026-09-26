package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request){
        List<String> messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        // Correção aqui: adicionamos a regra para quando houver campos duplicados
        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (mensagemExistente, novaMensagem) -> mensagemExistente + ", " + novaMensagem // 👈 Junta os erros do mesmo campo
                ));

        ErrorResponse error = new ErrorResponse (
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação nos dados enviados",
                request.getDescription(false).replace("uri=", ""),
                messages,
                details
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        return ResponseEntity
                .status(ex.getStatus())
                .body(ex.toErrorResponse(path));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        log.warn("Entrada inválida em {}: {}", path, ex.getMessage());
        String message = "Os dados enviados são inválidos.";

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                path,
                List.of(message),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        log.warn("Parâmetro obrigatório ausente em {}: {}", path, ex.getMessage());

        String message = String.format("O parâmetro obrigatório '%s' não foi informado.", ex.getParameterName());

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro obrigatório ausente",
                path,
                List.of(message),
                Map.of(ex.getParameterName(), "Este parâmetro é obrigatório.")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        log.error("Falha inesperada ao processar {}", path, ex);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                path,
                List.of("Não foi possível concluir a solicitação. Tente novamente."),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

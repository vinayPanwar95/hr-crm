package com.fms.hr_crm.lead.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(LeadNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(LeadNotFoundException ex) {
        log.warn("Lead not found: {}", ex.getMessage());
        return error("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateLeadException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateLeadException ex) {
        log.warn("Duplicate lead detected: {}", ex.getMessage());
        return error("DUPLICATE_LEAD", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleBusinessRule(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return error("BUSINESS_RULE_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        return new ErrorResponse("VALIDATION_ERROR",
                "Request validation failed", fieldErrors, Instant.now());
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse(code, message, Map.of(), Instant.now());
    }

    public record ErrorResponse(
            String code,
            String message,
            Map<String, String> fieldErrors,
            Instant timestamp
    ) {}
}
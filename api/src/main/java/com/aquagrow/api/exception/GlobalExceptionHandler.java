package com.aquagrow.api.exception;

import com.aquagrow.api.dto.ErrorResponse;
import com.aquagrow.api.security.TenantIsolationException;
import com.aquagrow.api.security.SubTenantAccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.validationError(
                "Validation failed. Please check the highlighted fields.",
                fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.businessError(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.businessError(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SubTenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSubTenantAccessDenied(
            SubTenantAccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.businessError(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TenantIsolationException.class)
    public ResponseEntity<ErrorResponse> handleTenantIsolation(
            TenantIsolationException ex, WebRequest request) {
        HttpStatus status = ex.getKind() == TenantIsolationException.Kind.NOT_FOUND
                ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN;
        return new ResponseEntity<>(ErrorResponse.businessError(ex.getMessage()), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {

        // Hibernate and Spring wrap exceptions raised inside entity callbacks (for example while committing),
        // so look through the causes before treating this as an unexpected error.
        TenantIsolationException isolation = findCause(ex, TenantIsolationException.class);
        if (isolation != null) {
            return handleTenantIsolation(isolation, request);
        }

        ErrorResponse errorResponse = ErrorResponse.systemError(
                "An unexpected error occurred. Please try again later.");

        log.error(errorResponse.getMessage(), ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 15; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return null;
    }
}

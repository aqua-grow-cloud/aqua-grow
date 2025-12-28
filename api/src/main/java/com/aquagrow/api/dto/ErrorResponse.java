package com.aquagrow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private ErrorType type;
    private Map<String, String> fieldErrors;

    public enum ErrorType {
        VALIDATION_ERROR,
        BUSINESS_ERROR,
        SYSTEM_ERROR
    }

    public static ErrorResponse validationError(String message, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .message(message)
                .type(ErrorType.VALIDATION_ERROR)
                .fieldErrors(fieldErrors)
                .build();
    }

    public static ErrorResponse businessError(String message) {
        return ErrorResponse.builder()
                .message(message)
                .type(ErrorType.BUSINESS_ERROR)
                .build();
    }

    public static ErrorResponse systemError(String message) {
        return ErrorResponse.builder()
                .message(message)
                .type(ErrorType.SYSTEM_ERROR)
                .build();
    }
}

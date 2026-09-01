package com.aquagrow.api.exception;

import com.aquagrow.api.security.SubTenantAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSubTenantAccess_returns403() {
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());
        SubTenantAccessDeniedException ex = new SubTenantAccessDeniedException("Only root tenant allowed");

        ResponseEntity<com.aquagrow.api.dto.ErrorResponse> response =
                handler.handleSubTenantAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Only root tenant allowed");
    }
}

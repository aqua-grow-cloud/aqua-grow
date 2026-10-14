package com.aquagrow.api.exception;

import com.aquagrow.api.security.SubTenantAccessDeniedException;
import com.aquagrow.api.security.TenantIsolationException;
import org.springframework.orm.jpa.JpaSystemException;
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

    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void anAccessToAnotherFarmsRecordIsAnsweredAsNotFound_withoutRevealingAnything() {
        ResponseEntity<com.aquagrow.api.dto.ErrorResponse> response =
                handler.handleTenantIsolation(new TenantIsolationException(TenantIsolationException.Kind.NOT_FOUND), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Not found");
    }

    @Test
    void aWriteToAnotherFarmIsForbidden() {
        ResponseEntity<com.aquagrow.api.dto.ErrorResponse> response =
                handler.handleTenantIsolation(new TenantIsolationException(TenantIsolationException.Kind.FORBIDDEN), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anIsolationErrorWrappedByHibernateOrSpringIsStillRecognised() {
        Exception wrapped = new JpaSystemException(new RuntimeException("callback failed",
                new TenantIsolationException(TenantIsolationException.Kind.NOT_FOUND)));

        ResponseEntity<com.aquagrow.api.dto.ErrorResponse> response = handler.handleGlobalException(wrapped, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void otherUnexpectedErrorsAreStillA500() {
        ResponseEntity<com.aquagrow.api.dto.ErrorResponse> response =
                handler.handleGlobalException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

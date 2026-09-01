package com.aquagrow.api.security;

public class SubTenantAccessDeniedException extends RuntimeException {
    public SubTenantAccessDeniedException(String message) {
        super(message);
    }
}

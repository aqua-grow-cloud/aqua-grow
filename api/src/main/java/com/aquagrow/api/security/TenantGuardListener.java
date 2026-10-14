package com.aquagrow.api.security;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/** JPA lifecycle callbacks that apply {@link TenantGuard} to every entity that lists this class as its listener. */
public class TenantGuardListener {

    @PostLoad
    public void afterLoad(Object entity) {
        TenantGuard.checkLoad(entity);
    }

    @PrePersist
    public void beforePersist(Object entity) {
        TenantGuard.checkPersist(entity);
    }

    @PreUpdate
    public void beforeUpdate(Object entity) {
        TenantGuard.checkWrite(entity, "update");
    }

    @PreRemove
    public void beforeRemove(Object entity) {
        TenantGuard.checkWrite(entity, "delete");
    }
}

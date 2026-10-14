package com.aquagrow.api.security;

import com.aquagrow.api.model.FeedStock;
import lombok.extern.slf4j.Slf4j;

/**
 * Enforces farm (tenant) ownership on every record, whichever way it reaches the database.
 *
 * Hibernate's tenant filter only applies to queries. A lookup by primary key (findById), the load that
 * precedes an update or delete, and a "save" of an object that carries an existing id all bypass it, so
 * without this guard any signed-in user could read, change or delete another farm's records by id.
 * The guard runs from {@link TenantGuardListener} on every load, insert, update and delete of every
 * entity, so a new endpoint cannot forget it.
 *
 * Rules, with {@code caller} the tenant of the signed-in user:
 * <ul>
 *   <li>loading a record that belongs to another farm is refused as "not found";</li>
 *   <li>creating, changing or deleting a record that belongs to another farm is refused;</li>
 *   <li>a new record without an owner is given the caller's farm;</li>
 *   <li>the one deliberate sharing: a sub-account may use its parent farm's feed stock;</li>
 *   <li>when no tenant is bound (startup, tests, background work) nothing is checked.</li>
 * </ul>
 */
@Slf4j
public final class TenantGuard {

    private TenantGuard() {
    }

    public static void checkLoad(Object entity) {
        if (!(entity instanceof TenantOwned owned)) {
            return;
        }
        String caller = TenantContext.getTenantId();
        if (caller == null || permits(owned, caller)) {
            return;
        }
        throw deny(owned, caller, "load", TenantIsolationException.Kind.NOT_FOUND);
    }

    public static void checkPersist(Object entity) {
        if (!(entity instanceof TenantOwned owned)) {
            return;
        }
        String caller = TenantContext.getTenantId();
        if (caller == null) {
            return;
        }
        if (owned.getTenantId() == null) {
            owned.setTenantId(caller);
            return;
        }
        if (!permits(owned, caller)) {
            throw deny(owned, caller, "create", TenantIsolationException.Kind.FORBIDDEN);
        }
    }

    public static void checkWrite(Object entity, String operation) {
        if (!(entity instanceof TenantOwned owned)) {
            return;
        }
        String caller = TenantContext.getTenantId();
        if (caller == null || permits(owned, caller)) {
            return;
        }
        throw deny(owned, caller, operation, TenantIsolationException.Kind.FORBIDDEN);
    }

    static boolean permits(TenantOwned entity, String caller) {
        String owner = entity.getTenantId();
        if (owner == null) {
            return false;
        }
        if (owner.equals(caller)) {
            return true;
        }
        // A sub-account works with its parent farm's feed stock (see TenantContext.getEffectiveFeedTenantId).
        return entity instanceof FeedStock && owner.equals(TenantContext.getEffectiveFeedTenantId());
    }

    private static TenantIsolationException deny(TenantOwned entity, String caller, String operation,
                                                 TenantIsolationException.Kind kind) {
        log.warn("AUDIT cross-farm access blocked: {} of {} (owned by tenant {}) by a user of tenant {}",
                operation, entity.getClass().getSimpleName(), entity.getTenantId(), caller);
        return new TenantIsolationException(kind);
    }
}

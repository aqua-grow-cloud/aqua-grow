package com.aquagrow.api.security;

/**
 * A request tried to read or change a record that belongs to another farm. The message is deliberately
 * the same as for a record that does not exist, so it reveals nothing about the other farm's data.
 */
public class TenantIsolationException extends RuntimeException {

    public enum Kind {
        /** The record was loaded by id (or looked up) but belongs to another farm: answer as "not found". */
        NOT_FOUND,
        /** The request tried to write something that belongs to another farm, or to hand a record to one. */
        FORBIDDEN
    }

    private final Kind kind;

    public TenantIsolationException(Kind kind) {
        super(kind == Kind.NOT_FOUND ? "Not found" : "This action is not allowed for your farm");
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}

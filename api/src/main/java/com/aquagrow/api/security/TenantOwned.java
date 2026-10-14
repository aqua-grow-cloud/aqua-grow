package com.aquagrow.api.security;

/** An entity that belongs to one farm (tenant). Every entity stored in the database implements this. */
public interface TenantOwned {

    String getTenantId();

    void setTenantId(String tenantId);
}

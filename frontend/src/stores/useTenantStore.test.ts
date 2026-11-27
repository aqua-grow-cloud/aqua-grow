import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { keycloak } from '@/plugins/keycloak'
import { useTenantStore } from './useTenantStore'

function setClaims(claims: Record<string, unknown> | undefined) {
  ;(keycloak as { tokenParsed?: unknown }).tokenParsed = claims
}

describe('useTenantStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    setClaims(undefined)
  })

  it('isRootTenant when no parent_tenant_id in token', () => {
    setClaims({ tenant_id: 'root-farm' })
    const store = useTenantStore()
    expect(store.isRootTenant).toBe(true)
    expect(store.isSubTenant).toBe(false)
    expect(store.parentTenantId).toBeNull()
  })

  it('isSubTenant when parent_tenant_id present in token', () => {
    setClaims({ tenant_id: 'branch-1', parent_tenant_id: 'root-farm' })
    const store = useTenantStore()
    expect(store.isSubTenant).toBe(true)
    expect(store.isRootTenant).toBe(false)
    expect(store.parentTenantId).toBe('root-farm')
  })

  it('isRootTenant when no token present', () => {
    const store = useTenantStore()
    expect(store.isRootTenant).toBe(true)
    expect(store.isSubTenant).toBe(false)
  })

  describe('tenantLabel', () => {
    it('is the tenant name for a root tenant', () => {
      setClaims({ tenant_id: 't1', tenant_name: 'Roopchand Farms' })
      expect(useTenantStore().tenantLabel).toBe('Roopchand Farms')
    })

    it('is "Parent / Tenant" for a sub-tenant with both names', () => {
      setClaims({
        tenant_id: 'b1',
        tenant_name: 'Branch 1',
        parent_tenant_id: 'p1',
        parent_tenant_name: 'Roopchand Farms',
      })
      expect(useTenantStore().tenantLabel).toBe('Roopchand Farms / Branch 1')
    })

    it('is only the tenant name for a sub-tenant without a parent name', () => {
      setClaims({ tenant_id: 'b1', tenant_name: 'Branch 1', parent_tenant_id: 'p1' })
      expect(useTenantStore().tenantLabel).toBe('Branch 1')
    })

    it('is null (never the raw id) when the token has no tenant_name', () => {
      setClaims({ tenant_id: 't1' })
      expect(useTenantStore().tenantLabel).toBeNull()
    })

    it('ignores a blank tenant_name', () => {
      setClaims({ tenant_id: 't1', tenant_name: '   ' })
      expect(useTenantStore().tenantLabel).toBeNull()
    })
  })

  it('refresh() picks up claims from a renewed token', () => {
    setClaims({ tenant_id: 't1' })
    const store = useTenantStore()
    expect(store.tenantLabel).toBeNull()
    setClaims({ tenant_id: 't1', tenant_name: 'New Name' })
    store.refresh()
    expect(store.tenantLabel).toBe('New Name')
  })
})

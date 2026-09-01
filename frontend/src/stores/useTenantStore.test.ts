import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTenantStore } from './useTenantStore'

// Helper to encode a fake JWT with given payload
function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'RS256', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.fakesig`
}

describe('useTenantStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('isRootTenant when no parent_tenant_id in token', () => {
    localStorage.setItem('auth_token', fakeJwt({ tenant_id: 'root-farm' }))
    const store = useTenantStore()
    expect(store.isRootTenant).toBe(true)
    expect(store.isSubTenant).toBe(false)
    expect(store.parentTenantId).toBeNull()
  })

  it('isSubTenant when parent_tenant_id present in token', () => {
    localStorage.setItem('auth_token', fakeJwt({ tenant_id: 'branch-1', parent_tenant_id: 'root-farm' }))
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
})

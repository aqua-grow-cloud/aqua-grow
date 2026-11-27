import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { keycloak } from '@/plugins/keycloak'
import TenantBadge from './TenantBadge.vue'

const stubs = {
  'v-chip': { template: '<span data-testid="tenant-badge"><slot /></span>' },
}

function setClaims(claims: Record<string, unknown> | undefined) {
  ;(keycloak as { tokenParsed?: unknown }).tokenParsed = claims
}

describe('TenantBadge', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    setClaims(undefined)
  })

  it('shows the tenant name from the token', () => {
    setClaims({ tenant_id: 't1', tenant_name: 'Roopchand Farms' })
    const wrapper = mount(TenantBadge, { global: { stubs } })
    expect(wrapper.find('[data-testid="tenant-badge"]').text()).toBe('Roopchand Farms')
  })

  it('shows "Parent / Tenant" for a sub-tenant', () => {
    setClaims({
      tenant_id: 'b1',
      tenant_name: 'Branch 1',
      parent_tenant_id: 'p1',
      parent_tenant_name: 'Roopchand Farms',
    })
    const wrapper = mount(TenantBadge, { global: { stubs } })
    expect(wrapper.text()).toBe('Roopchand Farms / Branch 1')
  })

  it('renders nothing when the token has no tenant name', () => {
    setClaims({ tenant_id: 't1' })
    const wrapper = mount(TenantBadge, { global: { stubs } })
    expect(wrapper.find('[data-testid="tenant-badge"]').exists()).toBe(false)
  })
})

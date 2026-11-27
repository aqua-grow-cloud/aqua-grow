import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { keycloak } from '@/plugins/keycloak'

/** Tenant claims added to the Keycloak access token. */
interface TenantClaims {
  tenant_id?: string
  tenant_name?: string
  parent_tenant_id?: string
  parent_tenant_name?: string
}

function readClaims(): TenantClaims {
  return (keycloak.tokenParsed ?? {}) as TenantClaims
}

export const useTenantStore = defineStore('tenant', () => {
  // Use a ref (not computed) so Vue tracks it as reactive state.
  // Call refresh() after a token update to pick up new claims mid-session.
  const claims = ref<TenantClaims>(readClaims())

  function refresh() {
    claims.value = readClaims()
  }

  const parentTenantId = computed<string | null>(
    () => claims.value.parent_tenant_id ?? null
  )

  const isSubTenant = computed(() => parentTenantId.value !== null)
  const isRootTenant = computed(() => parentTenantId.value === null)

  const tenantName = computed<string | null>(() => claims.value.tenant_name?.trim() || null)
  const parentTenantName = computed<string | null>(
    () => claims.value.parent_tenant_name?.trim() || null
  )

  /** Header text: "Parent / Tenant" for sub-tenants, the tenant name otherwise. Null when the token has no name. */
  const tenantLabel = computed<string | null>(() => {
    if (!tenantName.value) return null
    return isSubTenant.value && parentTenantName.value
      ? `${parentTenantName.value} / ${tenantName.value}`
      : tenantName.value
  })

  return {
    isSubTenant,
    isRootTenant,
    parentTenantId,
    tenantName,
    parentTenantName,
    tenantLabel,
    refresh,
  }
})

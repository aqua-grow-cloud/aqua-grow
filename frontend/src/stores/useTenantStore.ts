import { defineStore } from 'pinia'
import { computed } from 'vue'

interface JwtPayload {
  tenant_id?: string
  parent_tenant_id?: string
}

function decodeJwtPayload(token: string): JwtPayload {
  try {
    const base64Payload = token.split('.')[1]
    return JSON.parse(atob(base64Payload)) as JwtPayload
  } catch {
    return {}
  }
}

function getTokenPayload(): JwtPayload {
  const token = localStorage.getItem('auth_token')
  if (!token) return {}
  return decodeJwtPayload(token)
}

export const useTenantStore = defineStore('tenant', () => {
  const payload = computed(() => getTokenPayload())

  const parentTenantId = computed<string | null>(
    () => payload.value.parent_tenant_id ?? null
  )

  const isSubTenant = computed(() => parentTenantId.value !== null)
  const isRootTenant = computed(() => parentTenantId.value === null)

  return { isSubTenant, isRootTenant, parentTenantId }
})

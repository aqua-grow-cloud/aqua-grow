import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface JwtPayload {
  tenant_id?: string
  parent_tenant_id?: string
}

function decodeJwtPayload(token: string): JwtPayload {
  try {
    const base64Payload = token.split('.')[1]
    if (!base64Payload) return {}
    return JSON.parse(atob(base64Payload)) as JwtPayload
  } catch {
    return {}
  }
}

const AUTH_TOKEN_KEY = 'auth_token'

function readPayload(): JwtPayload {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (!token) return {}
  return decodeJwtPayload(token)
}

export const useTenantStore = defineStore('tenant', () => {
  // Use a ref (not computed) so Vue tracks it as reactive state.
  // Call refresh() after login to update mid-session if needed.
  const payload = ref<JwtPayload>(readPayload())

  function refresh() {
    payload.value = readPayload()
  }

  const parentTenantId = computed<string | null>(
    () => payload.value.parent_tenant_id ?? null
  )

  const isSubTenant = computed(() => parentTenantId.value !== null)
  const isRootTenant = computed(() => parentTenantId.value === null)

  return { isSubTenant, isRootTenant, parentTenantId, refresh }
})

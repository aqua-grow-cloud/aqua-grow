import { vi } from 'vitest'

// Mock Keycloak
vi.mock('@/plugins/keycloak', () => ({
  keycloak: {
    authenticated: true,
    token: 'mock-token',
    tokenParsed: {
      tenant_id: 'test-tenant',
      preferred_username: 'test-user',
    },
    updateToken: vi.fn().mockResolvedValue(true),
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

// Mock Vue Router
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
  })),
  useRoute: vi.fn(() => ({
    params: {},
    query: {},
    path: '/',
  })),
}))

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { usePonds } from './usePonds'
import { setActivePinia, createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import { createApp } from 'vue'

// Mock the pond service
vi.mock('../api/pondService', () => ({
  pondService: {
    useGetAll: vi.fn(() => ({
      data: {
        value: [
          { id: '1', name: 'Pond 1', status: 'Active', area: 100 },
          { id: '2', name: 'Pond 2', status: 'Inactive', area: 150 },
          { id: '3', name: 'Pond 3', status: 'Active', area: 200 },
        ],
      },
      isLoading: { value: false },
      error: { value: null },
      refetch: vi.fn(),
    })),
    useCreate: vi.fn(() => ({
      mutateAsync: vi.fn(),
      isPending: { value: false },
    })),
    useUpdate: vi.fn(() => ({
      mutateAsync: vi.fn(),
      isPending: { value: false },
    })),
    useDelete: vi.fn(() => ({
      mutateAsync: vi.fn(),
      isPending: { value: false },
    })),
  },
}))

describe('usePonds', () => {
  beforeEach(() => {
    // Setup Pinia
    const pinia = createPinia()
    setActivePinia(pinia)
    
    // Setup Vue Query
    const app = createApp({})
    app.use(VueQueryPlugin)
  })

  it('should return ponds data', () => {
    const { ponds } = usePonds()
    
    expect(ponds.value).toBeDefined()
    expect(ponds.value).toHaveLength(3)
    expect(ponds.value?.[0]?.name).toBe('Pond 1')
  })

  it('should filter active ponds correctly', () => {
    const { activePonds } = usePonds()
    
    expect(activePonds.value).toHaveLength(2)
    expect(activePonds.value.every((p: any) => p.status === 'Active')).toBe(true)
  })

  it('should filter inactive ponds correctly', () => {
    const { inactivePonds } = usePonds()
    
    expect(inactivePonds.value).toHaveLength(1)
    expect(inactivePonds.value?.[0]?.status).toBe('Inactive')
  })

  it('should find pond by id', () => {
    const { getPondById } = usePonds()
    
    const pond = getPondById('2')
    expect(pond).toBeDefined()
    expect(pond?.name).toBe('Pond 2')
  })

  it('should return undefined for non-existent pond id', () => {
    const { getPondById } = usePonds()
    
    const pond = getPondById('999')
    expect(pond).toBeUndefined()
  })

  it('should return undefined when id is not provided', () => {
    const { getPondById } = usePonds()
    
    const pond = getPondById()
    expect(pond).toBeUndefined()
  })

  it('should expose loading states', () => {
    const { isLoading, isCreating, isUpdating, isDeleting } = usePonds()
    
    expect(isLoading.value).toBe(false)
    expect(isCreating.value).toBe(false)
    expect(isUpdating.value).toBe(false)
    expect(isDeleting.value).toBe(false)
  })
})

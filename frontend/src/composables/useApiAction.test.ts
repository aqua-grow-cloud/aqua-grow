import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useApiAction } from './useApiAction'
import { setActivePinia, createPinia } from 'pinia'
import { useAquaStore } from '@/stores/useAquaStore'

describe('useApiAction', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should execute action successfully', async () => {
    const mockAction = vi.fn().mockResolvedValue({ id: '1', name: 'Test' })
    const { execute, data, isLoading, error } = useApiAction(mockAction)

    expect(isLoading.value).toBe(false)
    expect(data.value).toBeNull()

    await execute()

    expect(mockAction).toHaveBeenCalledOnce()
    expect(data.value).toEqual({ id: '1', name: 'Test' })
    expect(isLoading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  it('should show success notification by default', async () => {
    const mockAction = vi.fn().mockResolvedValue('success')
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction)
    await execute()

    expect(notifySpy).toHaveBeenCalledWith('Operation completed successfully', 'success')
  })

  it('should show custom success message', async () => {
    const mockAction = vi.fn().mockResolvedValue('success')
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction, {
      successMessage: 'Custom success message',
    })
    await execute()

    expect(notifySpy).toHaveBeenCalledWith('Custom success message', 'success')
  })

  it('should not show notification when disabled', async () => {
    const mockAction = vi.fn().mockResolvedValue('success')
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction, {
      showSuccessNotification: false,
    })
    await execute()

    expect(notifySpy).not.toHaveBeenCalled()
  })

  it('should handle validation errors', async () => {
    const mockError = {
      response: {
        data: {
          message: 'Validation failed',
          type: 'VALIDATION_ERROR',
          fieldErrors: {
            name: 'Name is required',
            email: 'Invalid email format',
          },
        },
      },
    }
    const mockAction = vi.fn().mockRejectedValue(mockError)
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute, error, fieldErrors } = useApiAction(mockAction)
    await execute()

    expect(error.value).toBe(mockError)
    expect(fieldErrors.value).toEqual({
      name: 'Name is required',
      email: 'Invalid email format',
    })
    expect(notifySpy).toHaveBeenCalledWith('Validation failed', 'businessError')
  })

  it('should handle business errors', async () => {
    const mockError = {
      response: {
        data: {
          message: 'Pond already exists',
          type: 'BUSINESS_ERROR',
        },
      },
    }
    const mockAction = vi.fn().mockRejectedValue(mockError)
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction)
    await execute()

    expect(notifySpy).toHaveBeenCalledWith('Pond already exists', 'businessError')
  })

  it('should handle system errors with retry callback', async () => {
    const mockError = {
      response: {
        data: {
          message: 'Database connection failed',
          type: 'SYSTEM_ERROR',
        },
      },
    }
    const mockAction = vi.fn().mockRejectedValue(mockError)
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction)
    await execute()

    expect(notifySpy).toHaveBeenCalledWith(
      'Database connection failed',
      'systemError',
      expect.any(Function)
    )
  })

  it('should handle errors without structured response', async () => {
    const mockError = new Error('Network error')
    const mockAction = vi.fn().mockRejectedValue(mockError)
    const store = useAquaStore()
    const notifySpy = vi.spyOn(store, 'notify')

    const { execute } = useApiAction(mockAction)
    await execute()

    expect(notifySpy).toHaveBeenCalledWith(
      'An unexpected error occurred. Please try again.',
      'systemError',
      expect.any(Function)
    )
  })

  it('should set loading state correctly', async () => {
    let resolveAction: (value: string) => void
    const mockAction = vi.fn(
      () =>
        new Promise<string>((resolve) => {
          resolveAction = resolve
        })
    )

    const { execute, isLoading } = useApiAction(mockAction)

    expect(isLoading.value).toBe(false)

    const promise = execute()
    expect(isLoading.value).toBe(true)

    resolveAction!('done')
    await promise

    expect(isLoading.value).toBe(false)
  })

  it('should clear errors on new execution', async () => {
    const mockError = {
      response: {
        data: {
          message: 'Error',
          type: 'VALIDATION_ERROR',
          fieldErrors: { name: 'Required' },
        },
      },
    }

    const mockAction = vi
      .fn()
      .mockRejectedValueOnce(mockError)
      .mockResolvedValueOnce('success')

    const { execute, error, fieldErrors } = useApiAction(mockAction, {
      showSuccessNotification: false,
    })

    // First execution - fails
    await execute()
    expect(error.value).toBe(mockError)
    expect(fieldErrors.value).toEqual({ name: 'Required' })

    // Second execution - succeeds
    await execute()
    expect(error.value).toBeNull()
    expect(fieldErrors.value).toEqual({})
  })
})

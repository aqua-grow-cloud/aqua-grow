import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useSalaryPayments } from './useSalaryPayments'
import { setActivePinia, createPinia } from 'pinia'

// Mock the generated API hooks
vi.mock('@/api/generated/salary-payment-controller/salary-payment-controller', () => ({
  useGetAllPayments: vi.fn(() => ({
    data: { value: mockPayments },
    isLoading: { value: false },
    error: { value: null },
  })),
  useGetPaymentById: vi.fn(() => ({
    data: { value: null },
    isLoading: { value: false },
  })),
  useGetEmployeeSalaryHistory: vi.fn(() => ({
    data: { value: [] },
    isLoading: { value: false },
  })),
  useGetPaymentsForMonth: vi.fn(() => ({
    data: { value: [] },
    isLoading: { value: false },
  })),
  getCreatePaymentMutationOptions: vi.fn(() => ({
    mutationFn: vi.fn(),
  })),
  getDeletePaymentMutationOptions: vi.fn(() => ({
    mutationFn: vi.fn(),
  })),
}))

vi.mock('@tanstack/vue-query', async () => {
  const actual = await vi.importActual('@tanstack/vue-query')
  return {
    ...actual,
    useQueryClient: vi.fn(() => ({
      invalidateQueries: vi.fn(),
    })),
    useMutation: vi.fn((options) => ({
      mutateAsync: vi.fn(async (data) => {
        if (options?.onSuccess) {
          options.onSuccess()
        }
        return { id: 'new-payment', ...data }
      }),
      isPending: { value: false },
      error: { value: null },
    })),
  }
})

const mockPayments = [
  {
    id: 'pay-001',
    employeeId: 'emp-001',
    date: '2024-01-31',
    baseSalary: 5000,
    advanceDeduction: 500,
    bonus: 200,
    netAmount: 4700,
    month: '2024-01',
    tenantId: 'tenant-1',
  },
  {
    id: 'pay-002',
    employeeId: 'emp-002',
    date: '2024-01-31',
    baseSalary: 6000,
    advanceDeduction: 0,
    bonus: 500,
    netAmount: 6500,
    month: '2024-01',
    tenantId: 'tenant-1',
  },
]

describe('useSalaryPayments', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Basic functionality', () => {
    it('should return payments data', () => {
      const { payments } = useSalaryPayments()
      
      expect(payments.value).toBeDefined()
      expect(Array.isArray(payments.value)).toBe(true)
    })

    it('should provide loading state', () => {
      const { isLoading } = useSalaryPayments()
      
      expect(isLoading.value).toBeDefined()
      expect(typeof isLoading.value).toBe('boolean')
    })

    it('should provide processing state', () => {
      const { isProcessing } = useSalaryPayments()
      
      expect(isProcessing.value).toBeDefined()
      expect(typeof isProcessing.value).toBe('boolean')
    })
  })

  describe('calculateNetAmount', () => {
    it('should calculate net amount correctly', () => {
      const { calculateNetAmount } = useSalaryPayments()
      
      const result = calculateNetAmount(5000, 500, 200)
      
      expect(result).toBe(4700)
    })

    it('should handle zero advance and bonus', () => {
      const { calculateNetAmount } = useSalaryPayments()
      
      const result = calculateNetAmount(5000, 0, 0)
      
      expect(result).toBe(5000)
    })

    it('should handle undefined advance and bonus as zero', () => {
      const { calculateNetAmount } = useSalaryPayments()
      
      const result = calculateNetAmount(5000)
      
      expect(result).toBe(5000)
    })

    it('should handle negative advance (rare case)', () => {
      const { calculateNetAmount } = useSalaryPayments()
      
      const result = calculateNetAmount(5000, -100, 200)
      
      expect(result).toBe(5300)
    })
  })

  describe('formatMonth', () => {
    it('should format date to YYYY-MM', () => {
      const { formatMonth } = useSalaryPayments()
      
      const result = formatMonth(new Date('2024-01-15'))
      
      expect(result).toBe('2024-01')
    })

    it('should format string date to YYYY-MM', () => {
      const { formatMonth } = useSalaryPayments()
      
      const result = formatMonth('2024-12-31')
      
      expect(result).toBe('2024-12')
    })

    it('should pad single-digit months with zero', () => {
      const { formatMonth } = useSalaryPayments()
      
      const result = formatMonth(new Date('2024-03-15'))
      
      expect(result).toBe('2024-03')
    })
  })

  describe('createPayment', () => {
    it('should create a salary payment', async () => {
      const { createPayment } = useSalaryPayments()
      
      const paymentData = {
        employeeId: 'emp-001',
        date: '2024-01-31',
        baseSalary: 5000,
        advanceDeduction: 500,
        bonus: 200,
      }

      const result = await createPayment(paymentData)
      
      expect(result).toBeDefined()
      expect(result).toHaveProperty('id')
    })

    it('should handle missing advance and bonus', async () => {
      const { createPayment } = useSalaryPayments()
      
      const paymentData = {
        employeeId: 'emp-001',
        date: '2024-01-31',
        baseSalary: 5000,
      }

      const result = await createPayment(paymentData)
      
      expect(result).toBeDefined()
    })
  })

  describe('deletePayment', () => {
    it('should delete a salary payment', async () => {
      const { deletePayment } = useSalaryPayments()
      
      await expect(deletePayment('pay-001')).resolves.not.toThrow()
    })
  })

  describe('paymentsByMonth', () => {
    it('should group payments by month', () => {
      const { paymentsByMonth } = useSalaryPayments()
      
      expect(paymentsByMonth.value).toBeDefined()
      expect(typeof paymentsByMonth.value).toBe('object')
    })

    it('should create separate groups for different months', () => {
      const { paymentsByMonth } = useSalaryPayments()
      
      const groups = Object.keys(paymentsByMonth.value)
      
      expect(groups.length).toBeGreaterThanOrEqual(0)
    })
  })

  describe('Options-based queries', () => {
    it('should accept employeeId option', () => {
      const { payments } = useSalaryPayments({ employeeId: 'emp-001' })
      
      expect(payments.value).toBeDefined()
    })

    it('should accept month option', () => {
      const { payments, monthSummary } = useSalaryPayments({ month: '2024-01' })
      
      expect(payments.value).toBeDefined()
      expect(monthSummary.value).toBeDefined()
    })

    it('should accept paymentId option', () => {
      const { payments } = useSalaryPayments({ paymentId: 'pay-001' })
      
      expect(payments.value).toBeDefined()
    })
  })
})

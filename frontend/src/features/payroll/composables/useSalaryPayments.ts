import { computed } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useGetAllPayments,
  useGetPaymentById,
  useGetEmployeeSalaryHistory,
  useGetPaymentsForMonth,
  getCreatePaymentMutationOptions,
  getDeletePaymentMutationOptions,
} from '@/api/generated/salary-payment-controller/salary-payment-controller'
import { useMutation } from '@tanstack/vue-query'
import type { SalaryPayment } from '@/api/generated/model'

/**
 * Salary Payments Composable
 * 
 * Manages salary payment operations using the generated API hooks.
 * Provides methods for:
 * - Fetching all payments, employee history, and monthly payments
 * - Creating new salary payments with automatic calculations
 * - Deleting payments (for corrections)
 * - Calculating net salary amounts
 * 
 * @example
 * ```typescript
 * const { payments, createPayment, calculateNetAmount } = useSalaryPayments()
 * 
 * // Create a payment
 * await createPayment({
 *   employeeId: 'emp-001',
 *   date: '2024-01-31',
 *   baseSalary: 5000,
 *   advanceDeduction: 500,
 *   bonus: 200
 * })
 * 
 * // Calculate net amount
 * const net = calculateNetAmount(5000, 500, 200) // Returns 4700
 * ```
 */
export function useSalaryPayments(options?: {
  employeeId?: string
  month?: string
  paymentId?: string
}) {
  const queryClient = useQueryClient()

  // Query hooks - conditionally enabled based on options
  const { data: allPayments, isLoading: isLoadingAll, error: errorAll } = 
    useGetAllPayments({ query: { enabled: !options?.employeeId && !options?.month } })

  const { data: employeeHistory, isLoading: isLoadingHistory } = 
    useGetEmployeeSalaryHistory(
      options?.employeeId || '', 
      { query: { enabled: !!options?.employeeId } }
    )

  const { data: monthlyPayments, isLoading: isLoadingMonth } = 
    useGetPaymentsForMonth(
      options?.month || '', 
      { query: { enabled: !!options?.month } }
    )

  const { data: singlePayment, isLoading: isLoadingSingle } = 
    useGetPaymentById(
      options?.paymentId || '', 
      { query: { enabled: !!options?.paymentId } }
    )

  // Mutation hooks
  const createMutation = useMutation({
    ...getCreatePaymentMutationOptions(),
    onSuccess: () => {
      // Invalidate all salary payment queries to refetch data
      queryClient.invalidateQueries({ queryKey: ['api', 'salary-payments'] })
      queryClient.invalidateQueries({ queryKey: ['api', 'employees'] })
    },
  })

  const deleteMutation = useMutation({
    ...getDeletePaymentMutationOptions(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api', 'salary-payments'] })
    },
  })

  // Computed values
  const payments = computed(() => {
    if (options?.employeeId) return employeeHistory.value || []
    if (options?.month) return monthlyPayments.value || []
    if (options?.paymentId) return singlePayment.value ? [singlePayment.value] : []
    return allPayments.value || []
  })

  const isLoading = computed(() => 
    isLoadingAll.value || isLoadingHistory.value || isLoadingMonth.value || isLoadingSingle.value
  )

  const isProcessing = computed(() => 
    createMutation.isPending.value || deleteMutation.isPending.value
  )

  // Helper functions
  /**
   * Calculate net salary amount
   * Formula: baseSalary - advanceDeduction + bonus
   */
  function calculateNetAmount(
    baseSalary: number,
    advanceDeduction: number = 0,
    bonus: number = 0
  ): number {
    return baseSalary - advanceDeduction + bonus
  }

  /**
   * Format month string from date (YYYY-MM)
   */
  function formatMonth(date: Date | string): string {
    const d = typeof date === 'string' ? new Date(date) : date
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    return `${year}-${month}`
  }

  /**
   * Get payment summary for a specific month
   */
  const monthSummary = computed(() => {
    if (!options?.month || !monthlyPayments.value) return null

    const total = monthlyPayments.value.reduce((sum, p) => sum + (p.netAmount || 0), 0)
    const count = monthlyPayments.value.length
    const avgSalary = count > 0 ? total / count : 0
    const totalAdvance = monthlyPayments.value.reduce((sum, p) => sum + (p.advanceDeduction || 0), 0)
    const totalBonus = monthlyPayments.value.reduce((sum, p) => sum + (p.bonus || 0), 0)

    return {
      month: options.month,
      totalPaid: total,
      employeeCount: count,
      averageSalary: avgSalary,
      totalAdvance,
      totalBonus,
    }
  })

  // Actions
  /**
   * Create a new salary payment
   */
  async function createPayment(paymentData: {
    employeeId: string
    date: string
    baseSalary: number
    advanceDeduction?: number
    bonus?: number
  }) {
    return createMutation.mutateAsync({
      data: {
        employeeId: paymentData.employeeId,
        date: paymentData.date,
        baseSalary: paymentData.baseSalary,
        advanceDeduction: paymentData.advanceDeduction || 0,
        bonus: paymentData.bonus || 0,
      },
    })
  }

  /**
   * Delete a salary payment (use with caution - for corrections only)
   */
  async function deletePayment(id: string) {
    return deleteMutation.mutateAsync({ id })
  }

  /**
   * Get payments grouped by month
   */
  const paymentsByMonth = computed(() => {
    if (!allPayments.value) return {}

    return allPayments.value.reduce((acc, payment) => {
      const month = payment.month || 'unknown'
      if (!acc[month]) {
        acc[month] = []
      }
      acc[month].push(payment)
      return acc
    }, {} as Record<string, SalaryPayment[]>)
  })

  return {
    // Data
    payments,
    monthSummary,
    paymentsByMonth,
    
    // Loading states
    isLoading,
    isProcessing,
    error: errorAll,
    
    // Actions
    createPayment,
    deletePayment,
    
    // Helpers
    calculateNetAmount,
    formatMonth,
    
    // Mutation states (for error handling)
    createError: createMutation.error,
    deleteError: deleteMutation.error,
  }
}

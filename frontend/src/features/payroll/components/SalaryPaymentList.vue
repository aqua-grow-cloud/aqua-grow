<template>
  <v-card class="rounded-lg">
    <v-card-title class="d-flex align-center px-6 pt-6">
      <span class="text-h6">Salary Payment History</span>
      <v-spacer></v-spacer>
      <v-chip v-if="monthFilter" closable @click:close="monthFilter = null" color="primary">
        {{ monthFilter }}
      </v-chip>
    </v-card-title>

    <v-card-text class="px-6">
      <!-- Month Filter -->
      <v-row class="mb-4">
        <v-col cols="12" md="4">
          <v-text-field
            v-model="monthFilter"
            label="Filter by Month"
            type="month"
            variant="outlined"
            density="compact"
            clearable
            prepend-inner-icon="mdi-calendar"
            hide-details
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="8" class="text-right">
          <div v-if="monthSummary" class="d-inline-flex gap-4">
            <v-chip variant="outlined">
              <v-icon start>mdi-account-group</v-icon>
              {{ monthSummary.employeeCount }} employees
            </v-chip>
            <v-chip variant="outlined" color="success">
              <v-icon start>mdi-cash</v-icon>
              Total: ₹{{ monthSummary.totalPaid.toLocaleString() }}
            </v-chip>
          </div>
        </v-col>
      </v-row>

      <!-- Loading State -->
      <v-progress-linear v-if="isLoading" indeterminate color="primary" class="mb-4"></v-progress-linear>

      <!-- Empty State -->
      <v-alert v-else-if="!payments.length" type="info" variant="tonal" class="mb-4">
        No salary payments found{{ monthFilter ? ' for this month' : '' }}.
      </v-alert>

      <!-- Payments Table -->
      <v-table v-else density="compact" class="rounded-lg">
        <thead>
          <tr>
            <th>Date</th>
            <th>Month</th>
            <th>Employee</th>
            <th class="text-right">Base Salary</th>
            <th class="text-right">Advance</th>
            <th class="text-right">Bonus</th>
            <th class="text-right">Net Amount</th>
            <th v-if="showActions" class="text-center">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="payment in payments" :key="payment.id">
            <td>{{ formatDate(payment.date) }}</td>
            <td>
              <v-chip size="small" variant="tonal">{{ payment.month }}</v-chip>
            </td>
            <td>
              <span class="font-weight-medium">{{ getEmployeeName(payment.employeeId) }}</span>
            </td>
            <td class="text-right">₹{{ payment.baseSalary?.toLocaleString() || 0 }}</td>
            <td class="text-right text-orange">
              {{ payment.advanceDeduction ? `-₹${payment.advanceDeduction.toLocaleString()}` : '—' }}
            </td>
            <td class="text-right text-success">
              {{ payment.bonus ? `+₹${payment.bonus.toLocaleString()}` : '—' }}
            </td>
            <td class="text-right font-weight-bold">₹{{ payment.netAmount?.toLocaleString() || 0 }}</td>
            <td v-if="showActions" class="text-center">
              <v-btn
                icon="mdi-delete"
                size="small"
                variant="text"
                color="error"
                @click="$emit('delete', payment.id)"
              ></v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>

      <!-- Summary Footer -->
      <div v-if="payments.length > 1" class="mt-4 pa-4 bg-grey-lighten-4 rounded-lg">
        <v-row dense>
          <v-col cols="12" sm="6" md="3">
            <div class="text-caption text-grey-darken-2">Total Payments</div>
            <div class="text-h6 font-weight-bold">{{ payments.length }}</div>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <div class="text-caption text-grey-darken-2">Total Base Salary</div>
            <div class="text-h6 font-weight-bold">₹{{ totalBaseSalary.toLocaleString() }}</div>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <div class="text-caption text-grey-darken-2">Total Deductions</div>
            <div class="text-h6 font-weight-bold text-orange">₹{{ totalDeductions.toLocaleString() }}</div>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <div class="text-caption text-grey-darken-2">Total Paid</div>
            <div class="text-h6 font-weight-bold text-success">₹{{ totalNetAmount.toLocaleString() }}</div>
          </v-col>
        </v-row>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useSalaryPayments } from '../composables/useSalaryPayments'
import type { SalaryPayment } from '@/api/generated/model'

const props = defineProps<{
  employeeId?: string
  employees?: Array<{ id: string; name: string }>
  showActions?: boolean
}>()

defineEmits<{
  (e: 'delete', id: string | undefined): void
}>()

const monthFilter = ref<string | null>(null)

// Create reactive options object
const queryOptions = computed(() => ({
  employeeId: props.employeeId,
  month: monthFilter.value || undefined
}))

const { payments, isLoading, monthSummary } = useSalaryPayments(queryOptions.value)

// Computed totals
const totalBaseSalary = computed(() => 
  payments.value.reduce((sum, p) => sum + (p.baseSalary || 0), 0)
)

const totalDeductions = computed(() => 
  payments.value.reduce((sum, p) => sum + (p.advanceDeduction || 0), 0)
)

const totalNetAmount = computed(() => 
  payments.value.reduce((sum, p) => sum + (p.netAmount || 0), 0)
)

// Helper functions
function formatDate(date: string | undefined): string {
  if (!date) return '—'
  return new Date(date).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function getEmployeeName(employeeId: string | undefined): string {
  if (!employeeId) return '—'
  if (!props.employees) return employeeId
  const employee = props.employees.find(e => e.id === employeeId)
  return employee?.name || employeeId
}
</script>

<style scoped>
.v-table {
  border: 1px solid rgb(var(--v-theme-grey-lighten-3));
}
</style>

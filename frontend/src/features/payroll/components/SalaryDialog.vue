<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="500px">
    <v-card class="rounded-lg">
      <v-card-title class="px-6 pt-6">
        <span class="text-h5">Process Salary</span>
      </v-card-title>

      <v-card-text class="px-6 pb-6" v-if="employee">
        <div class="mb-4 text-grey-darken-1">Paying salary for <strong>{{ employee.name }}</strong></div>

        <v-row>
          <v-col cols="12" sm="6">
             <v-text-field
                label="Base Salary"
                prefix="₹"
                :model-value="employee.salary"
                readonly
                variant="outlined"
                density="compact"
                hide-details
                bg-color="grey-lighten-4"
              ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6">
             <v-text-field
                label="Payment Month"
                type="month"
                v-model="month"
                variant="outlined"
                density="compact"
                hide-details
              ></v-text-field>
          </v-col>

          <v-col cols="12" sm="6">
             <v-text-field
                label="Advance Deduction"
                prefix="₹"
                v-model.number="deduction"
                type="number"
                variant="outlined"
                density="compact"
                hint="Deduct from outstanding advance"
                persistent-hint
                color="orange"
              ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6">
             <v-text-field
                label="Bonus / Extras"
                prefix="₹"
                v-model.number="bonus"
                type="number"
                variant="outlined"
                density="compact"
                hide-details
                color="success"
              ></v-text-field>
          </v-col>

          <v-col cols="12" class="mt-4">
              <v-sheet border rounded="lg" class="pa-4 bg-slate-50 d-flex justify-space-between align-center">
                  <div class="text-subtitle-1 font-weight-medium">Net Payable:</div>
                  <div class="text-h5 font-weight-bold text-primary">₹{{ netSalary.toLocaleString() }}</div>
              </v-sheet>
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="$emit('update:modelValue', false)" :disabled="isProcessing">Cancel</v-btn>
        <v-btn 
          color="primary" 
          variant="flat" 
          :disabled="netSalary < 0 || isProcessing" 
          :loading="isProcessing"
          @click="confirmPay"
        >
          Process Payment
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { Employee } from '../../../api/generated/model';
import { useSalaryPayments } from '../composables/useSalaryPayments';

const props = defineProps<{
  modelValue: boolean;
  employee?: Employee;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'success'): void;
}>();

const { createPayment, calculateNetAmount, isProcessing } = useSalaryPayments();

const month = ref(new Date().toISOString().substr(0, 7));
const deduction = ref(0);
const bonus = ref(0);

const netSalary = computed(() => {
    if (!props.employee) return 0;
    return calculateNetAmount(
      props.employee.salary || 0,
      deduction.value || 0,
      bonus.value || 0
    );
});

watch(() => props.modelValue, (val) => {
    if (val && props.employee) {
        // Default deduction is the full advance or 0
        deduction.value = Math.min(props.employee.advanceTaken || 0, props.employee.salary || 0);
        bonus.value = 0;
        month.value = new Date().toISOString().substr(0, 7);
    }
});

async function confirmPay() {
    if (!props.employee?.id) return;

    const paymentDate = new Date().toISOString().split('T')[0];
    
    // Create payment data object with explicit types
    const paymentData = {
        employeeId: props.employee.id,
        date: paymentDate,
        baseSalary: props.employee.salary || 0,
        advanceDeduction: deduction.value,
        bonus: bonus.value
    } as {
        employeeId: string
        date: string
        baseSalary: number
        advanceDeduction: number
        bonus: number
    };

    try {
        await createPayment(paymentData);

        // Success - no notification needed, just emit success
        emit('success');
        emit('update:modelValue', false);
    } catch (error) {
        console.error('Failed to process salary payment:', error);
        // Error handling - could add a snackbar or alert here
    }
}
</script>

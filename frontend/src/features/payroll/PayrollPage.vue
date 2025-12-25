<template>
  <v-container fluid>
    <PageHeader
      title="Payroll Management"
      :action-label="store.isManager ? 'Add Employee' : undefined"
      @action="openEmployeeDialog"
    />

    <v-row>
      <!-- Employee Cards -->
      <v-col v-for="emp in employees" :key="emp.id" cols="12" md="6" lg="4">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-item>
            <template v-slot:prepend>
              <v-avatar color="primary" variant="tonal" size="48" class="mr-3">
                <v-icon icon="mdi-account" color="primary"></v-icon>
              </v-avatar>
            </template>
            <v-card-title class="text-h6 pb-0">{{ emp.name }}</v-card-title>
            <v-card-subtitle>{{ emp.role }}</v-card-subtitle>
          </v-card-item>

          <v-divider class="mx-4 mb-2"></v-divider>

          <v-card-text class="pt-2">
            <div class="d-flex justify-space-between mb-2">
              <span class="text-grey-darken-1">Base Salary:</span>
              <span class="font-weight-bold">₹{{ (emp.salary || 0).toLocaleString() }}</span>
            </div>
            <div class="d-flex justify-space-between mb-2">
              <span class="text-grey-darken-1">Outstanding Advance:</span>
              <v-chip
                size="x-small"
                :color="(emp.advanceTaken || 0) > 0 ? 'warning' : 'success'"
                variant="flat"
              >
                ₹{{ (emp.advanceTaken || 0).toLocaleString() }}
              </v-chip>
            </div>
            <div class="d-flex justify-space-between">
              <span class="text-grey-darken-1">Status:</span>
              <span :class="emp.status === 'Active' ? 'text-success' : 'text-error'">{{ emp.status }}</span>
            </div>
          </v-card-text>

          <v-divider></v-divider>

          <v-card-actions v-if="store.isManager">
            <v-btn
              block
              color="primary"
              variant="flat"
              class="rounded-lg"
              prepend-icon="mdi-cash-check"
              @click="openSalaryDialog(emp)"
            >
              Pay Salary
            </v-btn>
          </v-card-actions>
          <v-card-actions v-if="store.isManager" class="pt-0">
             <v-btn
              variant="tonal"
              color="orange-darken-2"
              class="flex-grow-1"
              density="comfortable"
              prepend-icon="mdi-hand-coin"
              @click="openAdvanceDialog(emp)"
            >
              Advance
            </v-btn>
            <v-btn
              variant="text"
              color="grey"
              icon="mdi-pencil"
              @click="editEmployee(emp)"
            ></v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <!-- Dialogs -->
    <EmployeeFormDialog
      v-model="empDialog"
      :item="selectedEmp"
      @save="saveEmployee"
    />

    <SalaryDialog
      v-model="salaryDialog"
      :employee="selectedEmp"
      @pay="paySalary"
    />

    <v-dialog v-model="advanceDialog" max-width="400px">
        <v-card class="rounded-lg">
            <v-card-title>Issue Advance</v-card-title>
            <v-card-text>
                <div class="mb-4">Issuing advance to <strong>{{ selectedEmp?.name }}</strong></div>
                <v-text-field
                    v-model.number="advanceAmount"
                    label="Amount"
                    prefix="₹"
                    type="number"
                    variant="outlined"
                    density="compact"
                ></v-text-field>
            </v-card-text>
            <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn variant="text" @click="advanceDialog = false">Cancel</v-btn>
                <v-btn color="primary" variant="flat" @click="confirmAdvance">Confirm</v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>

  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import { usePayroll } from './composables/usePayroll';
import type { Employee } from '../../api/generated/model';
import type { SalaryPayment } from '../../types';
import PageHeader from '../../components/PageHeader.vue';
import EmployeeFormDialog from './components/EmployeeFormDialog.vue';
import SalaryDialog from './components/SalaryDialog.vue';

const store = useAquaStore();
const { employees, addEmployee, updateEmployee, addAdvance, processSalary } = usePayroll();

const empDialog = ref(false);
const salaryDialog = ref(false);
const advanceDialog = ref(false);
const selectedEmp = ref<Employee | undefined>(undefined);
const advanceAmount = ref(0);

function openEmployeeDialog() {
    selectedEmp.value = undefined;
    empDialog.value = true;
}

function editEmployee(emp: Employee) {
    selectedEmp.value = emp;
    empDialog.value = true;
}

function openSalaryDialog(emp: Employee) {
    selectedEmp.value = emp;
    salaryDialog.value = true;
}

function openAdvanceDialog(emp: Employee) {
    selectedEmp.value = emp;
    advanceAmount.value = 0;
    advanceDialog.value = true;
}

async function saveEmployee(empData: any) {
    if (selectedEmp.value && selectedEmp.value.id) {
        await updateEmployee(selectedEmp.value.id, empData);
    } else {
        await addEmployee({
            ...empData,
            advanceTaken: 0,
            joinedDate: new Date().toISOString().split('T')[0]
        });
    }
    empDialog.value = false;
}

async function paySalary(payment: Omit<SalaryPayment, 'id'>) {
    await processSalary(payment);
    salaryDialog.value = false;
}

async function confirmAdvance() {
    if (selectedEmp.value && selectedEmp.value.id && advanceAmount.value > 0) {
        await addAdvance(selectedEmp.value.id, advanceAmount.value);
        advanceDialog.value = false;
    }
}
</script>

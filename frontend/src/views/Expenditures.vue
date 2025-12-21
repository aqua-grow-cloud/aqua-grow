<template>
  <v-container fluid>
    <PageHeader 
        title="Expenditure Overview" 
        action-label="Log Expense"
        @action="openExpenseDialog"
    />

    <!-- Summary Cards -->
    <v-row class="mb-4">
        <v-col cols="12" sm="4">
            <v-card variant="tonal" color="primary" class="rounded-lg">
                <v-card-item title="Total Investment" :subtitle="grandTotalFormatted">
                    <template v-slot:prepend>
                        <v-icon icon="mdi-cash-multiple" size="32"></v-icon>
                    </template>
                </v-card-item>
            </v-card>
        </v-col>
    </v-row>

    <!-- Pivot Table -->
    <v-card variant="outlined" class="rounded-lg shadow-sm">
      <v-card-title class="px-6 py-4 bg-slate-50 border-b">
          <div class="d-flex align-center">
              <v-icon icon="mdi-finance" color="primary" class="mr-2"></v-icon>
              Investment Pivot Table
          </div>
      </v-card-title>
      
      <v-table hover>
        <thead>
          <tr class="bg-grey-lighten-4">
            <th class="text-subtitle-2 font-weight-bold">Batch</th>
            <th class="text-subtitle-2 font-weight-bold">MacroInvestment</th>
            <th class="text-subtitle-2 font-weight-bold">Type</th>
            <th class="text-subtitle-2 font-weight-bold text-right">SUM of Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in flattenedRows" :key="index">
            <td class="font-weight-bold text-primary">{{ row.isFirstBatch ? row.batchName : '' }}</td>
            <td class="font-weight-medium">{{ row.isFirstMacro ? row.macroName : '' }}</td>
            <td class="text-grey-darken-1">{{ row.type || '-' }}</td>
            <td class="text-right font-weight-bold">₹{{ row.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
          </tr>
        </tbody>
        <tfoot class="bg-slate-100 font-weight-bold">
            <tr>
                <td colspan="3" class="text-right">Grand Total:</td>
                <td class="text-right text-h6">₹{{ grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
            </tr>
        </tfoot>
      </v-table>

      <v-card-text v-if="flattenedRows.length === 0" class="text-center py-12 text-grey">
        <v-icon icon="mdi-database-off" size="48" class="mb-2 opacity-20"></v-icon>
        <div>No expenditure records found.</div>
      </v-card-text>
    </v-card>

    <!-- Expense Dialog -->
    <v-dialog v-model="dialog" max-width="500px">
        <v-card class="rounded-lg">
            <v-card-title>Log Expense</v-card-title>
            <v-card-text>
                 <v-alert type="info" variant="tonal" density="compact">Feature under development. Use mock data for testing.</v-alert>
            </v-card-text>
            <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn variant="text" @click="dialog = false">Close</v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>

  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();
const dialog = ref(false);

const openExpenseDialog = () => {
    dialog.value = true;
};

interface PivotRow {
    batchName: string;
    macroName: string;
    type: string;
    amount: number;
    isFirstBatch: boolean;
    isFirstMacro: boolean;
}

const flattenedRows = computed(() => {
  const rows: PivotRow[] = [];
  const groups: Record<string, Record<string, Record<string, number>>> = {};

  // Grouping
  store.expenditures.forEach(exp => {
    let batchDisplay = 'General / Unallocated';
    if (exp.batchId) {
      const batch = store.getBatchById(exp.batchId);
      batchDisplay = batch ? batch.stockingDate : 'Unknown Batch';
    }

    const macro = exp.macroInvestment || 'Unallocated';
    const subType = exp.subType || 'Other';

    if (!groups[batchDisplay]) groups[batchDisplay] = {};
    const bGroup = groups[batchDisplay]!;
    
    if (!bGroup[macro]) bGroup[macro] = {};
    const mGroup = bGroup[macro]!;
    
    if (!mGroup[subType]) mGroup[subType] = 0;
    mGroup[subType] += exp.amount;
  });

  // Flattening
  Object.keys(groups).forEach(batchName => {
      let isFirstBatch = true;
      const macros = groups[batchName];
      if (!macros) return;
      
      Object.keys(macros).forEach(macroName => {
          let isFirstMacro = true;
          const types = macros[macroName];
          if (!types) return;
          
          Object.keys(types).forEach(typeName => {
              rows.push({
                  batchName,
                  macroName,
                  type: typeName,
                  amount: types[typeName] || 0,
                  isFirstBatch: isFirstBatch,
                  isFirstMacro: isFirstMacro
              });
              isFirstBatch = false;
              isFirstMacro = false;
          });
      });
  });

  return rows;
});

const grandTotal = computed(() => {
    return store.expenditures.reduce((sum, exp) => sum + exp.amount, 0);
});

const grandTotalFormatted = computed(() => {
    return `₹${grandTotal.value.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
});

</script>

<style scoped>
.v-table th {
    background-color: #f8fafc !important;
}
.v-table td {
    height: 48px !important;
    vertical-align: middle;
}
</style>

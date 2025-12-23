<template>
  <v-container fluid>
    <PageHeader 
        title="Finance Dashboard" 
        :action-label="store.isManager ? 'Log Entry' : undefined"
        @action="dialog = true"
    />

    <v-row v-if="!store.isManager">
      <v-col cols="12">
        <v-alert type="error" variant="tonal" title="Access Denied">
          Only Managers and Admins can view or add financial data.
        </v-alert>
      </v-col>
    </v-row>

    <template v-else>
      <v-tabs v-model="tab" color="primary" class="mb-4 align-tabs-title">
        <v-tab value="expenditure">
          <v-icon start icon="mdi-minus-circle-outline"></v-icon>
          Outgoings (Expenditure)
        </v-tab>
        <v-tab value="income">
          <v-icon start icon="mdi-plus-circle-outline"></v-icon>
          Incomings (Payments)
        </v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <!-- Expenditure Tab -->
        <v-window-item value="expenditure">
          <v-card variant="outlined" class="rounded-lg shadow-sm">
            <v-card-title class="px-6 py-4 bg-slate-50 border-b">
                <div class="d-flex align-center">
                    <v-icon icon="mdi-finance" color="primary" class="mr-2"></v-icon>
                    Expenditure Pivot Table
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
                      <td colspan="3" class="text-right text-uppercase text-caption">Total Outgoings:</td>
                      <td class="text-right text-h6 text-error">₹{{ grandTotalExp.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
                  </tr>
              </tfoot>
            </v-table>
            <v-card-text v-if="flattenedRows.length === 0" class="text-center py-12 text-grey">
              <div>No expenditure records found.</div>
            </v-card-text>
          </v-card>
        </v-window-item>

        <!-- Income Tab -->
        <v-window-item value="income">
          <v-card variant="outlined" class="rounded-lg shadow-sm">
            <v-card-title class="px-6 py-4 bg-slate-50 border-b">
                <div class="d-flex align-center">
                    <v-icon icon="mdi-cash-plus" color="success" class="mr-2"></v-icon>
                    Incoming Payments (Harvest Revenue)
                </div>
            </v-card-title>
            
            <v-table hover>
              <thead>
                <tr class="bg-grey-lighten-4">
                  <th class="text-subtitle-2 font-weight-bold">Date</th>
                  <th class="text-subtitle-2 font-weight-bold">Source / Buyer</th>
                  <th class="text-subtitle-2 font-weight-bold">Harvest Details</th>
                  <th class="text-subtitle-2 font-weight-bold text-right">Amount Received</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in harvestLogs" :key="log.id">
                  <td>{{ formatDate(log.date) }}</td>
                  <td class="font-weight-medium">{{ log.buyer || 'Direct Sale' }}</td>
                  <td class="text-grey-darken-1">
                      {{ log.biomassWeight }}kg (Est. {{ log.count?.toLocaleString() || 0 }} pcs)
                      <div class="text-caption">Batch: {{ getBatchName(log.batchId) }}</div>
                  </td>
                  <td class="text-right font-weight-bold text-success text-h6">
                      ₹{{ (log.revenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-slate-100 font-weight-bold">
                  <tr>
                      <td colspan="3" class="text-right text-uppercase text-caption">Total Incomings:</td>
                      <td class="text-right text-h5 text-success">₹{{ grandTotalInc.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
                  </tr>
              </tfoot>
            </v-table>
            <v-card-text v-if="!harvestLogs || harvestLogs.length === 0" class="text-center py-12 text-grey">
              <v-icon icon="mdi-cash-off" size="48" class="mb-2 opacity-20"></v-icon>
              <div>No incoming payments recorded.</div>
            </v-card-text>
          </v-card>

          <!-- Summary Alert -->
          <v-alert
            v-if="grandTotalInc > grandTotalExp"
            type="success"
            variant="tonal"
            class="mt-6 rounded-lg"
            title="Positive Net Position"
          >
            Your total incomings (₹{{ grandTotalInc.toLocaleString() }}) exceed your recorded outgoings (₹{{ grandTotalExp.toLocaleString() }}).
            Net Position: <strong>₹{{ (grandTotalInc - grandTotalExp).toLocaleString() }}</strong>
          </v-alert>
        </v-window-item>
      </v-window>
    </template>

    <!-- Entry Dialog -->
    <ExpenditureDialog
      v-model="dialog"
      @save="handleSaveExpenditure"
    />

  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import PageHeader from '../components/PageHeader.vue';
import ExpenditureDialog from '../components/ExpenditureDialog.vue';
import { useExpenditures } from '../composables/useExpenditures';
import { useHarvestLogs } from '../composables/useHarvestLogs';
import { useBatches } from '../composables/useBatches';
import type { Expenditure } from '../types';
import type { Expenditure as ApiExpenditure, HarvestLog } from '../api/generated/model';

const store = useAquaStore();
const dialog = ref(false);
const tab = ref('expenditure');

// Use composables
const { expenditures } = useExpenditures();
const { harvestLogs } = useHarvestLogs();
const { getBatchById } = useBatches();

function handleSaveExpenditure(payload: Omit<Expenditure, 'id'>) {
    store.logExpenditure(payload);
}

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

  expenditures.value?.forEach((exp: ApiExpenditure) => {
    let batchDisplay = 'General / Unallocated';
    if (exp.batchId) {
      const batch = getBatchById(exp.batchId);
      batchDisplay = batch?.stockingDate || 'Unknown Batch';
    }

    const macro = exp.macroInvestment || 'Unallocated';
    const subType = exp.subType || 'Other';

    if (!groups[batchDisplay]) groups[batchDisplay] = {};
    const bGroup = groups[batchDisplay]!;
    if (!bGroup[macro]) bGroup[macro] = {};
    const mGroup = bGroup[macro]!;
    if (!mGroup[subType]) mGroup[subType] = 0;
    mGroup[subType] += exp.amount || 0;
  });

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
                  isFirstBatch,
                  isFirstMacro
              });
              isFirstBatch = false;
              isFirstMacro = false;
          });
      });
  });
  return rows;
});

const grandTotalExp = computed(() => expenditures.value?.reduce((sum: number, exp: ApiExpenditure) => sum + (exp.amount || 0), 0) || 0);
const grandTotalInc = computed(() => harvestLogs.value?.reduce((sum: number, log: HarvestLog) => sum + (log.revenue || 0), 0) || 0);

function formatDate(dateStr?: string) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function getBatchName(batchId?: string) {
    if (!batchId) return 'Unknown';
    const batch = getBatchById(batchId);
    return batch?.name || batch?.stockingDate || 'Unknown';
}
</script>

<style scoped>
.v-table th {
    background-color: #f8fafc !important;
}
.v-table td {
    height: 64px !important;
    vertical-align: middle;
}
</style>

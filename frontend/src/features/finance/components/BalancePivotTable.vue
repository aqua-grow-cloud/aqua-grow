<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center">
        <v-icon icon="mdi-scale-balance" color="primary" class="mr-2"></v-icon>
        Batchwise Balance Sheet
      </div>
    </v-card-title>

    <v-table hover>
      <thead>
        <tr class="bg-grey-lighten-4">
          <th class="text-subtitle-2 font-weight-bold">Batch</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Investment (Out)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Revenue (In)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Net Position</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in balanceRows" :key="row.batchId">
          <td class="font-weight-bold text-primary">{{ row.batchName }}</td>
          <td class="text-right text-error font-weight-medium">
            ₹{{ row.totalOut.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
          </td>
          <td class="text-right text-success font-weight-medium">
            ₹{{ row.totalIn.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
          </td>
          <td class="text-right font-weight-bold" :class="row.balance >= 0 ? 'text-success' : 'text-error'">
            ₹{{ row.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
          </td>
        </tr>
      </tbody>
      <tfoot class="bg-slate-100 font-weight-bold">
        <tr>
          <td class="text-right">Grand Total:</td>
          <td class="text-right text-error">₹{{ grandTotalOut.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
          <td class="text-right text-success">₹{{ grandTotalIn.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
          <td class="text-right" :class="totalBalance >= 0 ? 'text-success' : 'text-error'">
            ₹{{ totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
          </td>
        </tr>
      </tfoot>
    </v-table>

    <v-card-text v-if="balanceRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-database-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No financial records found to generate balance sheet.</div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useExpenditures } from '../composables/useExpenditures';
import { useHarvestLogs } from '../../harvesting/composables/useHarvestLogs';
import { useBatches } from '../../batches/composables/useBatches';

const { expenditures } = useExpenditures();
const { harvestLogs } = useHarvestLogs();
const { batches, getBatchById } = useBatches();

interface BalanceRow {
  batchId: string;
  batchName: string;
  totalOut: number;
  totalIn: number;
  balance: number;
}

const balanceRows = computed(() => {
  const map: Record<string, { totalOut: number; totalIn: number }> = {};

  // All batches from store to ensure we see them even if no data
  batches.value?.forEach(b => {
    if (b.id) {
      map[b.id] = { totalOut: 0, totalIn: 0 };
    }
  });

  // Aggregating Outgoings
  expenditures.value?.forEach(exp => {
    const bId = exp.batchId || 'unallocated';
    if (!map[bId]) map[bId] = { totalOut: 0, totalIn: 0 };
    map[bId].totalOut += exp.amount || 0;
  });

  // Aggregating Incomings
  harvestLogs.value?.forEach(log => {
    const bId = log.batchId || 'unallocated';
    if (!map[bId]) map[bId] = { totalOut: 0, totalIn: 0 };
    map[bId].totalIn += log.revenue || 0;
  });

  return Object.entries(map)
    .map(([bId, totals]) => {
      const batch = bId === 'unallocated' ? null : getBatchById(bId);
      return {
        batchId: bId,
        batchName: batch ? (batch.name || batch.stockingDate || 'Unnamed Batch') : 'General / Unallocated',
        totalOut: totals.totalOut,
        totalIn: totals.totalIn,
        balance: totals.totalIn - totals.totalOut
      };
    })
    .filter(row => row.totalOut > 0 || row.totalIn > 0) // Only show batches with financial activity
    .sort((a, b) => b.totalOut + b.totalIn - (a.totalOut + a.totalIn)); // Sort by activity volume
});

const grandTotalOut = computed(() => balanceRows.value.reduce((s, r) => s + r.totalOut, 0));
const grandTotalIn = computed(() => balanceRows.value.reduce((s, r) => s + r.totalIn, 0));
const totalBalance = computed(() => grandTotalIn.value - grandTotalOut.value);
</script>

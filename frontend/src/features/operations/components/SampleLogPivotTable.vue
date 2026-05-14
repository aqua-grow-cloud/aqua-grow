<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon icon="mdi-fish" color="primary" class="mr-2"></v-icon>
          Sample Log Growth Analysis
        </div>
        <div class="d-flex gap-2 align-center">
          <v-btn
            variant="outlined"
            prepend-icon="mdi-download"
            color="primary"
            density="compact"
            class="rounded-lg"
            @click="exportPivotCsv"
          >
            Export Analysis
          </v-btn>
        </div>
      </div>
    </v-card-title>

    <v-table hover fixed-header height="600px">
      <thead>
        <tr class="bg-grey-lighten-4">
          <th class="text-subtitle-2 font-weight-bold">Batch/Pond</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Fish Sampled</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Avg Weight (g)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Min Weight (g)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Max Weight (g)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Growth Rate</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="row in displayRows" :key="row.batchId">
          <tr @click="toggleExpand(row.batchId)" class="cursor-pointer hover:bg-grey-lighten-5">
            <td class="font-weight-bold text-primary">
              <v-icon v-if="row.entries.length > 0" :icon="expandedRows.has(row.batchId) ? 'mdi-chevron-down' : 'mdi-chevron-right'" size="small" class="mr-1"></v-icon>
              {{ row.label }}
            </td>
            <td class="text-right">{{ row.totalFishSampled }}</td>
            <td class="text-right font-weight-medium">
              {{ row.weightedAvgWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right text-grey-darken-1">
              {{ row.minWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right text-grey-darken-1">
              {{ row.maxWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right">
              <v-chip v-if="row.growthRate !== null" size="small" :color="getGrowthColor(row.growthRate!)">
                {{ row.growthRate! > 0 ? '+' : '' }}{{ row.growthRate!.toFixed(2) }} g/day
              </v-chip>
              <span v-else class="text-caption text-grey">N/A</span>
            </td>
          </tr>
          <!-- Expanded children rows (chronological sample entries) -->
          <template v-if="expandedRows.has(row.batchId) && row.entries.length > 0">
            <tr v-for="entry in row.entries" :key="`${row.batchId}-${entry.date}-${entry.dayNumber}`" class="bg-grey-lighten-5">
              <td class="pl-12 text-grey-darken-2">
                <v-icon icon="mdi-calendar" size="x-small" class="mr-1"></v-icon>
                {{ formatDate(entry.date) }}
              </td>
              <td class="text-right">{{ entry.sampleCount }}</td>
              <td class="text-right">
                {{ entry.averageWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
              </td>
              <td class="text-right">-</td>
              <td class="text-right">-</td>
              <td class="text-right">
                <span class="text-caption">Day {{ entry.dayNumber }}</span>
              </td>
            </tr>
          </template>
        </template>
      </tbody>
      <tfoot class="bg-slate-100 font-weight-bold">
        <tr>
          <td class="text-right">Total Samples:</td>
          <td class="text-right">{{ totalFishSampled }}</td>
          <td class="text-right">{{ overallAvgWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g</td>
          <td class="text-right">-</td>
          <td class="text-right">-</td>
          <td class="text-right">-</td>
        </tr>
      </tfoot>
    </v-table>

    <v-card-text v-if="!isLoading && displayRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No sample log records found to generate growth analysis.</div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {sampleLogService, type SamplePivotRow} from '../api/sampleLogService';
import {useBatches} from '../../batches/composables/useBatches';
import {usePonds} from '../../ponds/composables/usePonds';

const { data: pivotData, isLoading } = sampleLogService.useGetPivot();
const { getBatchById } = useBatches();
const { getPondById } = usePonds();

const expandedRows = ref(new Set<string>());

// Enrich each pivot row with a display label resolved on the frontend
interface DisplayRow extends SamplePivotRow {
  label: string;
}

function getBatchLabel(batchId?: string): string {
  if (!batchId || batchId === 'unallocated') return 'Unallocated';
  const batch = getBatchById(batchId);
  if (!batch) return 'Unknown Batch';
  const pondNames = [...new Set(
    (batch.fishSeeds || [])
      .map((s: any) => getPondById(s.pondId)?.name)
      .filter(Boolean)
  )].join(', ') || 'Unknown Pond';
  return `${batch.name || batch.stockingDate || batchId} (${pondNames})`;
}

function formatDate(dateStr?: string | null): string {
  if (!dateStr) return 'Unknown Date';
  return new Date(dateStr).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

const displayRows = computed<DisplayRow[]>(() => {
  if (!pivotData.value) return [];
  return pivotData.value
    .map(row => ({ ...row, label: getBatchLabel(row.batchId) }))
    .sort((a, b) => b.weightedAvgWeight - a.weightedAvgWeight);
});

const totalFishSampled = computed(() =>
  displayRows.value.reduce((sum, r) => sum + r.totalFishSampled, 0)
);

const overallAvgWeight = computed(() => {
  const totalWeight = displayRows.value.reduce((sum, r) => sum + r.weightedAvgWeight * r.totalFishSampled, 0);
  return totalFishSampled.value > 0 ? totalWeight / totalFishSampled.value : 0;
});

function toggleExpand(key: string) {
  if (expandedRows.value.has(key)) {
    expandedRows.value.delete(key);
  } else {
    expandedRows.value.add(key);
  }
}

function getGrowthColor(growthRate: number): string {
  if (growthRate >= 1.0) return 'success';
  if (growthRate >= 0.5) return 'info';
  if (growthRate >= 0) return 'warning';
  return 'error';
}

function exportPivotCsv() {
  const rows = [
    ['Batch/Pond', 'Fish Sampled', 'Weighted Avg Weight (g)', 'Min ABW (g)', 'Max ABW (g)', 'Growth Rate (g/day)']
  ];

  displayRows.value.forEach(row => {
    rows.push([
      row.label,
      row.totalFishSampled.toString(),
      row.weightedAvgWeight.toFixed(2),
      row.minWeight.toFixed(2),
      row.maxWeight.toFixed(2),
      row.growthRate !== null ? row.growthRate!.toFixed(2) : 'N/A'
    ]);
    row.entries.forEach(e => {
      rows.push([
        `  → ${formatDate(e.date)}`,
        e.sampleCount.toString(),
        e.averageWeight.toFixed(2),
        '-',
        '-',
        `Day ${e.dayNumber}`
      ]);
    });
  });

  rows.push(['Total', totalFishSampled.value.toString(), overallAvgWeight.value.toFixed(2), '-', '-', '-']);

  const csvContent = rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.setAttribute('href', URL.createObjectURL(blob));
  link.setAttribute('download', `sample-log-growth-analysis-${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
.v-table th {
  background-color: #f8fafc !important;
}
.v-table td {
  height: 48px !important;
  vertical-align: middle;
}
</style>

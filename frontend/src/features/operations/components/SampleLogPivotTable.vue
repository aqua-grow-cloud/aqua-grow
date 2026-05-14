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
        <template v-for="(row, index) in pivotRows" :key="index">
          <tr @click="toggleExpand(row.key)" class="cursor-pointer hover:bg-grey-lighten-5">
            <td class="font-weight-bold text-primary">
              <v-icon v-if="row.children.length > 0" :icon="expandedRows.has(row.key) ? 'mdi-chevron-down' : 'mdi-chevron-right'" size="small" class="mr-1"></v-icon>
              {{ row.label }}
            </td>
            <td class="text-right">{{ row.count }}</td>
            <td class="text-right font-weight-medium">
              {{ row.avgWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right text-grey-darken-1">
              {{ row.minWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right text-grey-darken-1">
              {{ row.maxWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
            </td>
            <td class="text-right">
              <v-chip v-if="row.growthRate !== null" size="small" :color="getGrowthColor(row.growthRate)">
                {{ row.growthRate > 0 ? '+' : '' }}{{ row.growthRate.toFixed(2) }} g/day
              </v-chip>
              <span v-else class="text-caption text-grey">N/A</span>
            </td>
          </tr>
          <!-- Expanded children rows (chronological sample dates) -->
          <template v-if="expandedRows.has(row.key) && row.children.length > 0">
            <tr v-for="child in row.children" :key="child.key" class="bg-grey-lighten-5">
              <td class="pl-12 text-grey-darken-2">
                <v-icon icon="mdi-calendar" size="x-small" class="mr-1"></v-icon>
                {{ child.label }}
              </td>
              <td class="text-right">{{ child.count }}</td>
              <td class="text-right">
                {{ child.avgWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g
              </td>
              <td class="text-right">-</td>
              <td class="text-right">-</td>
              <td class="text-right">
                <span class="text-caption">Day {{ child.dayNumber }}</span>
              </td>
            </tr>
          </template>
        </template>
      </tbody>
      <tfoot class="bg-slate-100 font-weight-bold">
        <tr>
          <td class="text-right">Total Samples:</td>
          <td class="text-right">{{ totalCount }}</td>
          <td class="text-right">{{ overallAvgWeight.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} g</td>
          <td class="text-right">-</td>
          <td class="text-right">-</td>
          <td class="text-right">-</td>
        </tr>
      </tfoot>
    </v-table>

    <v-card-text v-if="pivotRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No sample log records found to generate growth analysis.</div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {sampleLogService} from '../api/sampleLogService';
import {useBatches} from '../../batches/composables/useBatches';
import {usePonds} from '../../ponds/composables/usePonds';
import type {SampleLog} from '@/api/generated/model';

const { data: sampleLogsResponse } = sampleLogService.useGetAll();
const { getBatchById } = useBatches();
const { getPondById } = usePonds();

const expandedRows = ref(new Set<string>());

// Extract sample logs from paginated response
const sampleLogs = computed(() => {
  return sampleLogsResponse.value?.content || [];
});

interface PivotRow {
  key: string;
  label: string;
  count: number;       // total fish sampled (sum of sampleCount)
  avgWeight: number;   // weighted avg: ΣtotalSampleWeight / ΣsampleCount
  minWeight: number;
  maxWeight: number;
  growthRate: number | null;
  children: ChildRow[];
}

interface ChildRow {
  key: string;
  label: string;
  count: number;
  avgWeight: number;
  dayNumber: number;
}

// Helper: batch label with pond names derived from fishSeeds
function getBatchLabel(batchId?: string): string {
  if (!batchId) return 'Unallocated';
  const batch = getBatchById(batchId);
  if (!batch) return 'Unknown Batch';

  const pondNames = [...new Set(
    (batch.fishSeeds || [])
      .map((s: any) => getPondById(s.pondId)?.name)
      .filter(Boolean)
  )].join(', ') || 'Unknown Pond';

  return `${batch.name || batch.stockingDate || batchId} (${pondNames})`;
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return 'Unknown Date';
  return new Date(dateStr).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

// Days between two date strings (floor, not ceil; result 0 for same day)
function daysBetween(startDate: string, endDate: string): number {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();
  return Math.floor((end - start) / (1000 * 60 * 60 * 24));
}

// Main pivot computation - grouped by batch
const pivotRows = computed(() => {
  if (!sampleLogs.value || sampleLogs.value.length === 0) return [];

  const groupMap: Record<string, { items: SampleLog[], groupLabel?: string }> = {};

  sampleLogs.value.forEach((log: SampleLog) => {
    const groupKey = log.batchId || 'unallocated';
    if (!groupMap[groupKey]) {
      groupMap[groupKey] = { items: [], groupLabel: getBatchLabel(log.batchId) };
    }
    groupMap[groupKey]!.items.push(log);
  });

  return Object.entries(groupMap)
    .map(([key, data]) => {
      // Sort samples chronologically
      const sortedSamples = [...data.items].sort((a, b) =>
        new Date(a.date || '').getTime() - new Date(b.date || '').getTime()
      );

      // Total fish sampled (sum of sampleCount across all records)
      const totalFishSampled = sortedSamples.reduce((sum, s) => sum + (s.sampleCount || 0), 0);

      // Weighted average ABW: Σ(totalSampleWeight) / Σ(sampleCount)
      // Fall back to averageWeight * sampleCount when totalSampleWeight is missing
      const totalWeight = sortedSamples.reduce((sum, s) => {
        const w = s.totalSampleWeight ?? ((s.averageWeight ?? 0) * (s.sampleCount ?? 1));
        return sum + w;
      }, 0);
      const avgWeight = totalFishSampled > 0 ? totalWeight / totalFishSampled : 0;

      // min/max are per-record averageWeight values
      const perRecordABW = sortedSamples.map(s => s.averageWeight || 0).filter(w => w > 0);
      const minWeight = perRecordABW.length > 0 ? Math.min(...perRecordABW) : 0;
      const maxWeight = perRecordABW.length > 0 ? Math.max(...perRecordABW) : 0;

      // Growth rate: (lastABW - firstABW) / days between first and last sample
      let growthRate: number | null = null;
      if (sortedSamples.length >= 2) {
        const firstSample = sortedSamples[0]!;
        const lastSample = sortedSamples[sortedSamples.length - 1]!;
        const firstWeight = firstSample.averageWeight || 0;
        const lastWeight = lastSample.averageWeight || 0;
        const days = daysBetween(firstSample.date || '', lastSample.date || '');
        if (days > 0 && firstWeight > 0 && lastWeight > 0) {
          growthRate = (lastWeight - firstWeight) / days;
        }
      }

      // Build children — each row is one sample record
      const firstSampleDate = sortedSamples[0]?.date || '';
      const children: ChildRow[] = sortedSamples.map((sample) => {
        const dayNumber = firstSampleDate ? daysBetween(firstSampleDate, sample.date || '') : 0;
        return {
          key: `${key}-${sample.id || sample.date}`,
          label: formatDate(sample.date),
          count: sample.sampleCount || 0,
          avgWeight: sample.averageWeight || 0,
          dayNumber,
        };
      });

      return {
        key,
        label: data.groupLabel || key,
        count: totalFishSampled,
        avgWeight,
        minWeight,
        maxWeight,
        growthRate,
        children,
      };
    })
    .sort((a, b) => b.avgWeight - a.avgWeight);
});

// Footer: weighted average across all logs
const totalFishSampled = computed(() =>
  sampleLogs.value?.reduce((sum: number, s: SampleLog) => sum + (s.sampleCount || 0), 0) ?? 0
);

const totalCount = computed(() => totalFishSampled.value);

const overallAvgWeight = computed(() => {
  if (!sampleLogs.value || totalFishSampled.value === 0) return 0;
  const totalW = sampleLogs.value.reduce((sum: number, s: SampleLog) => {
    const w = (s as any).totalSampleWeight ?? ((s.averageWeight ?? 0) * (s.sampleCount ?? 1));
    return sum + w;
  }, 0);
  return totalW / totalFishSampled.value;
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

  pivotRows.value.forEach(row => {
    rows.push([
      row.label,
      row.count.toString(),
      row.avgWeight.toFixed(2),
      row.minWeight.toFixed(2),
      row.maxWeight.toFixed(2),
      row.growthRate !== null ? row.growthRate.toFixed(2) : 'N/A'
    ]);

    row.children.forEach(child => {
      rows.push([
        `  → ${child.label}`,
        child.count.toString(),
        child.avgWeight.toFixed(2),
        '-',
        '-',
        `Day ${child.dayNumber}`
      ]);
    });
  });

  rows.push([
    'Total',
    totalCount.value.toString(),
    overallAvgWeight.value.toFixed(2),
    '-',
    '-',
    '-'
  ]);

  const csvContent = rows.map(row => row.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.setAttribute('href', url);
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

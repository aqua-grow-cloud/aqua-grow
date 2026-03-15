<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon icon="mdi-food-apple" color="primary" class="mr-2"></v-icon>
          Feed Log Pivot Analysis
        </div>
        <div class="d-flex gap-2 align-center">
          <v-select
            v-model="groupBy"
            :items="groupByOptions"
            label="Group By"
            density="compact"
            variant="outlined"
            hide-details
            class="w-48"
          ></v-select>
          <v-btn
            variant="outlined"
            prepend-icon="mdi-download"
            color="primary"
            density="compact"
            class="rounded-lg"
            @click="exportPivotCsv"
          >
            Export Pivot
          </v-btn>
        </div>
      </div>
    </v-card-title>

    <v-table hover fixed-header height="600px">
      <thead>
        <tr class="bg-grey-lighten-4">
          <th class="text-subtitle-2 font-weight-bold">{{ groupByLabel }}</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Total Amount (kg)</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Feedings</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Avg/Feeding</th>
          <th class="text-subtitle-2 font-weight-bold text-right">% of Total</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="(row, index) in pivotRows" :key="index">
          <tr @click="toggleExpand(row.key)" class="cursor-pointer hover:bg-grey-lighten-5">
            <td class="font-weight-bold text-primary">
              <v-icon v-if="row.children.length > 0" :icon="expandedRows.has(row.key) ? 'mdi-chevron-down' : 'mdi-chevron-right'" size="small" class="mr-1"></v-icon>
              {{ row.label }}
            </td>
            <td class="text-right font-weight-medium">
              {{ row.total.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg
            </td>
            <td class="text-right">{{ row.count }}</td>
            <td class="text-right text-grey-darken-1">
              {{ row.avg.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg
            </td>
            <td class="text-right">
              <v-chip size="small" :color="getPercentageColor(row.percentage)">
                {{ row.percentage.toFixed(1) }}%
              </v-chip>
            </td>
          </tr>
          <!-- Expanded children rows -->
          <template v-if="expandedRows.has(row.key) && row.children.length > 0">
            <tr v-for="child in row.children" :key="child.key" class="bg-grey-lighten-5">
              <td class="pl-12 text-grey-darken-2">
                <v-icon icon="mdi-subdirectory-arrow-right" size="x-small" class="mr-1"></v-icon>
                {{ child.label }}
              </td>
              <td class="text-right">
                {{ child.total.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg
              </td>
              <td class="text-right">{{ child.count }}</td>
              <td class="text-right text-grey-darken-1">
                {{ child.avg.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg
              </td>
              <td class="text-right">
                <span class="text-caption">{{ child.percentage.toFixed(1) }}%</span>
              </td>
            </tr>
          </template>
        </template>
      </tbody>
      <tfoot class="bg-slate-100 font-weight-bold">
        <tr>
          <td class="text-right">Grand Total:</td>
          <td class="text-right text-error text-h6">{{ grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg</td>
          <td class="text-right">{{ totalCount }}</td>
          <td class="text-right">{{ avgAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }} kg</td>
          <td class="text-right">100%</td>
        </tr>
      </tfoot>
    </v-table>

    <v-card-text v-if="pivotRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No feed log records found to generate pivot analysis.</div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {feedLogService} from '../api/feedLogService';
import {useBatches} from '../../batches/composables/useBatches';
import {usePonds} from '../../ponds/composables/usePonds';
import type {FeedLog} from '@/api/generated/model';

const { data: feedLogs } = feedLogService.useGetAll();
const { getBatchById } = useBatches();
const { getPondById } = usePonds();

const groupBy = ref('date');
const expandedRows = ref(new Set<string>());

const groupByOptions = [
  { title: 'Date', value: 'date' },
  { title: 'Batch/Pond', value: 'batch' },
  { title: 'Feed Type', value: 'feedType' },
];

const groupByLabel = computed(() => {
  const option = groupByOptions.find(o => o.value === groupBy.value);
  return option?.title || 'Group';
});

interface PivotRow {
  key: string;
  label: string;
  total: number;
  count: number;
  avg: number;
  percentage: number;
  children: PivotRow[];
}

// Helper function to format batch name with pond
function getBatchLabel(batchId?: string): string {
  if (!batchId) return 'Unallocated';
  const batch = getBatchById(batchId);
  if (!batch) return 'Unknown Batch';
  
  const pond = getPondById(batch.pondId);
  const pondName = pond?.name || 'Unknown Pond';
  const batchName = batch.name || batch.stockingDate || 'Unknown Batch';
  
  return `${batchName} (${pondName})`;
}

// Helper function to format date
function formatDate(dateStr?: string): string {
  if (!dateStr) return 'Unknown Date';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

// Main pivot computation
const pivotRows = computed(() => {
  if (!feedLogs.value || feedLogs.value.length === 0) return [];

  const groupMap: Record<string, { items: FeedLog[], subGroups?: Record<string, FeedLog[]>, subGroupLabels?: Record<string, string>, groupLabel?: string }> = {};
  const total = grandTotal.value;

  feedLogs.value.forEach((log: FeedLog) => {
    let groupKey = '';
    let groupLabel = '';
    let subGroupKey = '';
    let subGroupLabel = '';

    switch (groupBy.value) {
      case 'date':
        groupKey = log.date || 'unknown';
        groupLabel = formatDate(log.date);
        // Sub-group by feed type
        subGroupKey = log.feedType || 'Unknown Type';
        subGroupLabel = log.feedType || 'Unknown Type';
        break;
      case 'batch':
        groupKey = log.batchId || 'unallocated';
        groupLabel = getBatchLabel(log.batchId);
        // Sub-group by feed type
        subGroupKey = log.feedType || 'Unknown Type';
        subGroupLabel = log.feedType || 'Unknown Type';
        break;
      case 'feedType':
        groupKey = log.feedType || 'Unknown Type';
        groupLabel = log.feedType || 'Unknown Type';
        // Sub-group by batch
        subGroupKey = log.batchId || 'unallocated';
        subGroupLabel = getBatchLabel(log.batchId);
        break;
    }

    if (!groupMap[groupKey]) {
      groupMap[groupKey] = { items: [], subGroups: {}, subGroupLabels: {}, groupLabel: groupLabel };
    }
    groupMap[groupKey]!.items.push(log);

    // Add to subgroup
    if (subGroupKey && groupMap[groupKey]) {
      const group = groupMap[groupKey]!;
      if (!group.subGroups) group.subGroups = {};
      if (!group.subGroupLabels) group.subGroupLabels = {};
      if (!group.subGroups[subGroupKey]) {
        group.subGroups[subGroupKey] = [];
      }
      // Store the label for this subGroupKey
      group.subGroupLabels[subGroupKey] = subGroupLabel;
      group.subGroups[subGroupKey]!.push(log);
    }
  });

  return Object.entries(groupMap)
    .map(([key, data]) => {
      const rowTotal = data.items.reduce((sum, item) => sum + (item.amount || 0), 0);
      const rowCount = data.items.length;

      // Build children from subgroups
      const children: PivotRow[] = data.subGroups
        ? Object.entries(data.subGroups).map(([subKey, subItems]) => {
            const subTotal = subItems.reduce((sum, item) => sum + (item.amount || 0), 0);
            const subCount = subItems.length;
            // Use the stored label instead of the key
            const subLabel = data.subGroupLabels?.[subKey] || subKey;
            return {
              key: `${key}-${subKey}`,
              label: subLabel,
              total: subTotal,
              count: subCount,
              avg: subTotal / subCount,
              percentage: total > 0 ? (subTotal / total) * 100 : 0,
              children: []
            };
          }).sort((a, b) => b.total - a.total)
        : [];

      return {
        key,
        label: data.groupLabel || key,
        total: rowTotal,
        count: rowCount,
        avg: rowTotal / rowCount,
        percentage: total > 0 ? (rowTotal / total) * 100 : 0,
        children
      };
    })
    .sort((a, b) => {
      // For date grouping, sort chronologically (most recent first)
      if (groupBy.value === 'date') {
        return b.key.localeCompare(a.key);
      }
      // For others, sort by total amount
      return b.total - a.total;
    });
});

const grandTotal = computed(() =>
  feedLogs.value?.reduce((sum, log) => sum + (log.amount || 0), 0) || 0
);

const totalCount = computed(() => feedLogs.value?.length || 0);

const avgAmount = computed(() =>
  totalCount.value > 0 ? grandTotal.value / totalCount.value : 0
);

function toggleExpand(key: string) {
  if (expandedRows.value.has(key)) {
    expandedRows.value.delete(key);
  } else {
    expandedRows.value.add(key);
  }
}

function getPercentageColor(percentage: number): string {
  if (percentage >= 30) return 'error';
  if (percentage >= 15) return 'warning';
  if (percentage >= 5) return 'info';
  return 'default';
}

function exportPivotCsv() {
  const rows = [
    [groupByLabel.value, 'Total Amount (kg)', 'Feedings', 'Avg/Feeding (kg)', '% of Total']
  ];

  pivotRows.value.forEach(row => {
    rows.push([
      row.label,
      row.total.toFixed(2),
      row.count.toString(),
      row.avg.toFixed(2),
      row.percentage.toFixed(1) + '%'
    ]);

    // Add children
    row.children.forEach(child => {
      rows.push([
        '  → ' + child.label,
        child.total.toFixed(2),
        child.count.toString(),
        child.avg.toFixed(2),
        child.percentage.toFixed(1) + '%'
      ]);
    });
  });

  rows.push([
    'Grand Total',
    grandTotal.value.toFixed(2),
    totalCount.value.toString(),
    avgAmount.value.toFixed(2),
    '100%'
  ]);

  const csvContent = rows.map(row => row.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  link.setAttribute('href', url);
  link.setAttribute('download', `feed-log-pivot-${groupBy.value}-${new Date().toISOString().split('T')[0]}.csv`);
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

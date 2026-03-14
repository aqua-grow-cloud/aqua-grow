<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon icon="mdi-table-pivot" color="primary" class="mr-2"></v-icon>
          Expenditure Pivot Analysis
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
          <th class="text-subtitle-2 font-weight-bold text-right">Total Amount</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Count</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Avg Amount</th>
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
              ₹{{ row.total.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
            </td>
            <td class="text-right">{{ row.count }}</td>
            <td class="text-right text-grey-darken-1">
              ₹{{ row.avg.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
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
                ₹{{ child.total.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
              </td>
              <td class="text-right">{{ child.count }}</td>
              <td class="text-right text-grey-darken-1">
                ₹{{ child.avg.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
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
          <td class="text-right text-error text-h6">₹{{ grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
          <td class="text-right">{{ totalCount }}</td>
          <td class="text-right">₹{{ avgAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
          <td class="text-right">100%</td>
        </tr>
      </tfoot>
    </v-table>

    <v-card-text v-if="pivotRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No expenditure records found to generate pivot analysis.</div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useExpenditures} from '../composables/useExpenditures';
import {useBatches} from '../../batches/composables/useBatches';
import type {Expenditure} from '@/api/generated/model';

const { expenditures } = useExpenditures();
const { getBatchById } = useBatches();

const groupBy = ref('batch');
const expandedRows = ref(new Set<string>());

const groupByOptions = [
  { title: 'Batch', value: 'batch' },
  { title: 'Macro Investment', value: 'macro' },
  { title: 'Category/Type', value: 'category' },
  { title: 'Sub-Type', value: 'subType' },
  { title: 'Month', value: 'month' },
  { title: 'Year', value: 'year' },
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

// Helper function to format batch name
function getBatchName(batchId?: string): string {
  if (!batchId) return 'General / Unallocated';
  const batch = getBatchById(batchId);
  return batch?.name || batch?.stockingDate || 'Unknown Batch';
}

// Helper function to format date by grouping type
function formatDateGroup(dateStr?: string, groupType: string = 'month'): string {
  if (!dateStr) return 'Unknown';
  const date = new Date(dateStr);

  if (groupType === 'month') {
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });
  } else if (groupType === 'year') {
    return date.getFullYear().toString();
  }
  return dateStr;
}

// Main pivot computation
const pivotRows = computed(() => {
  if (!expenditures.value || expenditures.value.length === 0) return [];

  const groupMap: Record<string, { items: Expenditure[], subGroups?: Record<string, Expenditure[]>, subGroupLabels?: Record<string, string>, groupLabel?: string }> = {};
  const total = grandTotal.value;

  expenditures.value.forEach((exp: Expenditure) => {
    let groupKey = '';
    let groupLabel = '';
    let subGroupKey = '';
    let subGroupLabel = '';

    switch (groupBy.value) {
      case 'batch':
        groupKey = exp.batchId || 'unallocated';
        groupLabel = getBatchName(exp.batchId);
        // Sub-group by macro investment
        subGroupKey = exp.macroInvestment || 'Other';
        subGroupLabel = exp.macroInvestment || 'Other';
        break;
      case 'macro':
        groupKey = exp.macroInvestment || 'Other';
        groupLabel = exp.macroInvestment || 'Other';
        // Sub-group by category
        subGroupKey = exp.category || 'Other';
        subGroupLabel = exp.category || 'Other';
        break;
      case 'category':
        groupKey = exp.category || 'Other';
        groupLabel = exp.category || 'Other';
        // Sub-group by subType
        subGroupKey = exp.subType || 'None';
        subGroupLabel = exp.subType || 'None';
        break;
      case 'subType':
        groupKey = exp.subType || 'None';
        groupLabel = exp.subType || 'None';
        // Sub-group by batch
        subGroupKey = exp.batchId || 'unallocated';
        subGroupLabel = getBatchName(exp.batchId);
        break;
      case 'month':
        groupKey = formatDateGroup(exp.date as any, 'month');
        groupLabel = groupKey;
        // Sub-group by macro investment
        subGroupKey = exp.macroInvestment || 'Other';
        subGroupLabel = exp.macroInvestment || 'Other';
        break;
      case 'year':
        groupKey = formatDateGroup(exp.date as any, 'year');
        groupLabel = groupKey;
        // Sub-group by month
        subGroupKey = formatDateGroup(exp.date as any, 'month');
        subGroupLabel = subGroupKey;
        break;
    }

    if (!groupMap[groupKey]) {
      groupMap[groupKey] = { items: [], subGroups: {}, subGroupLabels: {}, groupLabel: groupLabel };
    }
    groupMap[groupKey]!.items.push(exp);

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
      group.subGroups[subGroupKey]!.push(exp);
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
    .sort((a, b) => b.total - a.total);
});

const grandTotal = computed(() =>
  expenditures.value?.reduce((sum, exp) => sum + (exp.amount || 0), 0) || 0
);

const totalCount = computed(() => expenditures.value?.length || 0);

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
    [groupByLabel.value, 'Total Amount', 'Count', 'Avg Amount', '% of Total']
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
  link.setAttribute('download', `expenditure-pivot-${groupBy.value}-${new Date().toISOString().split('T')[0]}.csv`);
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

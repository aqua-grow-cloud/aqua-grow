<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center justify-space-between w-100">
        <div class="d-flex align-center">
          <v-icon icon="mdi-table-pivot" color="primary" class="mr-2"></v-icon>
          Expenditure Pivot Analysis
        </div>
        <div class="d-flex gap-2 align-center">
          <v-btn
            variant="text"
            :prepend-icon="showFilters ? 'mdi-filter-off' : 'mdi-filter'"
            density="compact"
            @click="showFilters = !showFilters"
          >
            {{ showFilters ? 'Hide Filters' : 'Filters' }}
            <v-badge v-if="activeFilterCount > 0" :content="activeFilterCount" color="primary" inline></v-badge>
          </v-btn>
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

    <!-- Filter Panel -->
    <v-expand-transition>
      <div v-if="showFilters" class="px-6 py-4 bg-blue-grey-lighten-5 border-b">
        <div class="text-subtitle-2 font-weight-bold mb-3 d-flex align-center gap-2">
          <v-icon icon="mdi-filter-outline" size="small"></v-icon>
          Filters
          <v-btn v-if="activeFilterCount > 0" variant="text" size="x-small" color="error" @click="clearFilters">Clear All</v-btn>
        </div>
        <v-row dense>
          <v-col cols="12" sm="6" md="3">
            <v-text-field
              v-model="filters.dateFrom"
              label="Date From"
              type="date"
              density="compact"
              variant="outlined"
              hide-details
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-text-field
              v-model="filters.dateTo"
              label="Date To"
              type="date"
              density="compact"
              variant="outlined"
              hide-details
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-select
              v-model="filters.batchIds"
              :items="batchOptions"
              label="Batch"
              density="compact"
              variant="outlined"
              hide-details
              multiple
              clearable
              chips
              closable-chips
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-select
              v-model="filters.macroInvestments"
              :items="macroOptions"
              label="Macro Investment"
              density="compact"
              variant="outlined"
              hide-details
              multiple
              clearable
              chips
              closable-chips
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-select
              v-model="filters.categories"
              :items="categoryOptions"
              label="Category / Type"
              density="compact"
              variant="outlined"
              hide-details
              multiple
              clearable
              chips
              closable-chips
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-select
              v-model="filters.subTypes"
              :items="subTypeOptions"
              label="Sub-Type"
              density="compact"
              variant="outlined"
              hide-details
              multiple
              clearable
              chips
              closable-chips
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-text-field
              v-model.number="filters.amountMin"
              label="Min Amount (₹)"
              type="number"
              density="compact"
              variant="outlined"
              hide-details
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-text-field
              v-model.number="filters.amountMax"
              label="Max Amount (₹)"
              type="number"
              density="compact"
              variant="outlined"
              hide-details
              clearable
            ></v-text-field>
          </v-col>
        </v-row>
        <div class="mt-2 text-caption text-grey">
          Showing {{ filteredExpenditures.length }} of {{ expenditures?.length || 0 }} records
        </div>
      </div>
    </v-expand-transition>

    <!-- Group By Controls -->
    <div class="px-6 py-3 border-b bg-white">
      <div class="d-flex align-center gap-3 flex-wrap">
        <span class="text-subtitle-2 font-weight-bold text-no-wrap">Group By:</span>
        <v-select
          v-model="groupByLevels"
          :items="groupByOptions"
          label="Select groupings (ordered)"
          density="compact"
          variant="outlined"
          hide-details
          multiple
          chips
          closable-chips
          class="flex-grow-1"
          style="max-width: 600px"
        >
          <template #chip="{ props: chipProps, item }">
            <v-chip v-bind="chipProps" color="primary" size="small">
              <span class="mr-1 text-caption opacity-60">{{ groupByLevels.indexOf(item.value as string) + 1 }}.</span>
              {{ item.title }}
            </v-chip>
          </template>
        </v-select>
        <v-btn
          v-if="groupByLevels.length === 0"
          variant="tonal"
          size="small"
          color="primary"
          @click="groupByLevels = ['batch']"
        >
          Default (Batch)
        </v-btn>
      </div>
    </div>

    <v-table hover fixed-header height="600px">
      <thead>
        <tr class="bg-grey-lighten-4">
          <th class="text-subtitle-2 font-weight-bold">Group</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Total Amount</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Count</th>
          <th class="text-subtitle-2 font-weight-bold text-right">Avg Amount</th>
          <th class="text-subtitle-2 font-weight-bold text-right">% of Total</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="row in flatRows" :key="row.key">
          <tr
            :class="[
              'pivot-row',
              row.depth === 0 ? 'pivot-row--root' : '',
              row.hasChildren ? 'cursor-pointer' : ''
            ]"
            @click="row.hasChildren ? toggleExpand(row.key) : undefined"
          >
            <td :style="{ paddingLeft: `${16 + row.depth * 24}px` }">
              <v-icon
                v-if="row.hasChildren"
                :icon="expandedRows.has(row.key) ? 'mdi-chevron-down' : 'mdi-chevron-right'"
                size="small"
                class="mr-1"
              ></v-icon>
              <v-icon
                v-else-if="row.depth > 0"
                icon="mdi-subdirectory-arrow-right"
                size="x-small"
                class="mr-1 text-grey"
              ></v-icon>
              <span :class="row.depth === 0 ? 'font-weight-bold text-primary' : row.depth === 1 ? 'font-weight-medium' : 'text-grey-darken-2 text-caption'">
                {{ row.label }}
              </span>
              <v-chip v-if="row.depth === 0 && groupByLevels.length > 0" size="x-small" class="ml-2 opacity-60" variant="tonal">
                {{ groupByOptions.find(o => o.value === groupByLevels[0])?.title }}
              </v-chip>
            </td>
            <td class="text-right" :class="row.depth === 0 ? 'font-weight-bold' : ''">
              ₹{{ row.total.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
            </td>
            <td class="text-right">{{ row.count }}</td>
            <td class="text-right text-grey-darken-1">
              ₹{{ row.avg.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
            </td>
            <td class="text-right">
              <v-chip v-if="row.depth === 0" size="small" :color="getPercentageColor(row.percentage)">
                {{ row.percentage.toFixed(1) }}%
              </v-chip>
              <span v-else class="text-caption">{{ row.percentage.toFixed(1) }}%</span>
            </td>
          </tr>
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

    <v-card-text v-if="flatRows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No expenditure records found{{ activeFilterCount > 0 ? ' matching the current filters' : '' }}.</div>
      <v-btn v-if="activeFilterCount > 0" variant="text" color="primary" class="mt-2" @click="clearFilters">Clear Filters</v-btn>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from 'vue';
import {useExpenditures} from '../composables/useExpenditures';
import {useBatches} from '../../batches/composables/useBatches';
import type {Expenditure} from '@/api/generated/model';

const { expenditures } = useExpenditures();
const { getBatchById } = useBatches();

// ─── Group By ───────────────────────────────────────────────────────────────

const groupByOptions = [
  { title: 'Batch', value: 'batch' },
  { title: 'Macro Investment', value: 'macro' },
  { title: 'Category / Type', value: 'category' },
  { title: 'Sub-Type', value: 'subType' },
  { title: 'Month', value: 'month' },
  { title: 'Year', value: 'year' },
];

const groupByLevels = ref<string[]>(['batch']);
const expandedRows = ref(new Set<string>());

// ─── Filters ─────────────────────────────────────────────────────────────────

const showFilters = ref(false);

const filters = reactive({
  dateFrom: '',
  dateTo: '',
  batchIds: [] as string[],
  macroInvestments: [] as string[],
  categories: [] as string[],
  subTypes: [] as string[],
  amountMin: null as number | null,
  amountMax: null as number | null,
});

function clearFilters() {
  filters.dateFrom = '';
  filters.dateTo = '';
  filters.batchIds = [];
  filters.macroInvestments = [];
  filters.categories = [];
  filters.subTypes = [];
  filters.amountMin = null;
  filters.amountMax = null;
}

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.dateFrom) count++;
  if (filters.dateTo) count++;
  if (filters.batchIds.length) count++;
  if (filters.macroInvestments.length) count++;
  if (filters.categories.length) count++;
  if (filters.subTypes.length) count++;
  if (filters.amountMin !== null) count++;
  if (filters.amountMax !== null) count++;
  return count;
});

// ─── Filter Options (derived from data) ──────────────────────────────────────

const batchOptions = computed(() => {
  const ids = new Set(expenditures.value?.map(e => e.batchId).filter(Boolean));
  return Array.from(ids).map(id => ({
    title: getBatchName(id),
    value: id as string,
  }));
});

const macroOptions = computed(() =>
  [...new Set(expenditures.value?.map(e => e.macroInvestment).filter(Boolean))].sort() as string[]
);

const categoryOptions = computed(() =>
  [...new Set(expenditures.value?.map(e => e.category).filter(Boolean))].sort() as string[]
);

const subTypeOptions = computed(() =>
  [...new Set(expenditures.value?.map(e => e.subType).filter(Boolean))].sort() as string[]
);

// ─── Filtered Data ────────────────────────────────────────────────────────────

const filteredExpenditures = computed<Expenditure[]>(() => {
  if (!expenditures.value) return [];
  return expenditures.value.filter(exp => {
    if (filters.dateFrom && exp.date && exp.date < filters.dateFrom) return false;
    if (filters.dateTo && exp.date && exp.date > filters.dateTo) return false;
    if (filters.batchIds.length && !filters.batchIds.includes(exp.batchId || '')) return false;
    if (filters.macroInvestments.length && !filters.macroInvestments.includes(exp.macroInvestment || '')) return false;
    if (filters.categories.length && !filters.categories.includes(exp.category || '')) return false;
    if (filters.subTypes.length && !filters.subTypes.includes(exp.subType || '')) return false;
    if (filters.amountMin !== null && (exp.amount || 0) < filters.amountMin) return false;
    if (filters.amountMax !== null && (exp.amount || 0) > filters.amountMax) return false;
    return true;
  });
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

function getBatchName(batchId?: string): string {
  if (!batchId) return 'General / Unallocated';
  const batch = getBatchById(batchId);
  return batch?.name || batch?.stockingDate || batchId;
}

function formatDateGroup(dateStr?: string, groupType: 'month' | 'year' = 'month'): string {
  if (!dateStr) return 'Unknown';
  const date = new Date(dateStr);
  if (groupType === 'month') {
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });
  }
  return date.getFullYear().toString();
}

function getGroupKey(exp: Expenditure, level: string): { key: string; label: string } {
  switch (level) {
    case 'batch':
      return { key: exp.batchId || 'unallocated', label: getBatchName(exp.batchId) };
    case 'macro':
      return { key: exp.macroInvestment || 'Other', label: exp.macroInvestment || 'Other' };
    case 'category':
      return { key: exp.category || 'Other', label: exp.category || 'Other' };
    case 'subType':
      return { key: exp.subType || 'None', label: exp.subType || 'None' };
    case 'month': {
      const label = formatDateGroup(exp.date as any, 'month');
      return { key: label, label };
    }
    case 'year': {
      const label = formatDateGroup(exp.date as any, 'year');
      return { key: label, label };
    }
    default:
      return { key: 'Other', label: 'Other' };
  }
}

// ─── Pivot Tree ───────────────────────────────────────────────────────────────

interface PivotNode {
  key: string;
  label: string;
  total: number;
  count: number;
  avg: number;
  percentage: number;
  depth: number;
  hasChildren: boolean;
  children: PivotNode[];
}

function buildTree(items: Expenditure[], levels: string[], depth: number, grandTotalRef: number, parentKey = ''): PivotNode[] {
  if (levels.length === 0 || items.length === 0) return [];

  const [currentLevel, ...restLevels] = levels as [string, ...string[]];
  const groupMap = new Map<string, { label: string; items: Expenditure[] }>();

  items.forEach(exp => {
    const { key, label } = getGroupKey(exp, currentLevel);
    if (!groupMap.has(key)) {
      groupMap.set(key, { label, items: [] });
    }
    groupMap.get(key)!.items.push(exp);
  });

  return Array.from(groupMap.entries())
    .map(([key, data]) => {
      const nodeKey = parentKey ? `${parentKey}::${key}` : key;
      const total = data.items.reduce((sum, e) => sum + (e.amount || 0), 0);
      const count = data.items.length;
      const children = buildTree(data.items, restLevels, depth + 1, grandTotalRef, nodeKey);

      return {
        key: nodeKey,
        label: data.label,
        total,
        count,
        avg: count > 0 ? total / count : 0,
        percentage: grandTotalRef > 0 ? (total / grandTotalRef) * 100 : 0,
        depth,
        hasChildren: children.length > 0,
        children,
      };
    })
    .sort((a, b) => b.total - a.total);
}

const pivotTree = computed<PivotNode[]>(() => {
  const items = filteredExpenditures.value;
  const levels = groupByLevels.value.length > 0 ? groupByLevels.value : ['batch'];
  const total = items.reduce((sum, e) => sum + (e.amount || 0), 0);
  return buildTree(items, levels, 0, total);
});

// ─── Flat rows for rendering (respecting expand state) ────────────────────────

interface FlatRow extends Omit<PivotNode, 'children'> {
  hasChildren: boolean;
}

function flattenTree(nodes: PivotNode[]): FlatRow[] {
  const result: FlatRow[] = [];
  for (const node of nodes) {
    const { children, ...rest } = node;
    result.push({ ...rest, hasChildren: children.length > 0 });
    if (expandedRows.value.has(node.key) && children.length > 0) {
      result.push(...flattenTree(children));
    }
  }
  return result;
}

const flatRows = computed(() => flattenTree(pivotTree.value));

// ─── Aggregates ───────────────────────────────────────────────────────────────

const grandTotal = computed(() =>
  filteredExpenditures.value.reduce((sum, exp) => sum + (exp.amount || 0), 0)
);

const totalCount = computed(() => filteredExpenditures.value.length);

const avgAmount = computed(() =>
  totalCount.value > 0 ? grandTotal.value / totalCount.value : 0
);

// ─── Actions ──────────────────────────────────────────────────────────────────

function toggleExpand(key: string) {
  const next = new Set(expandedRows.value);
  if (next.has(key)) {
    // Collapse this node and all its descendants
    for (const k of [...next]) {
      if (k === key || k.startsWith(`${key}::`)) next.delete(k);
    }
  } else {
    next.add(key);
  }
  expandedRows.value = next;
}

function getPercentageColor(percentage: number): string {
  if (percentage >= 30) return 'error';
  if (percentage >= 15) return 'warning';
  if (percentage >= 5) return 'info';
  return 'default';
}

// ─── CSV Export ───────────────────────────────────────────────────────────────

function flattenForCsv(nodes: PivotNode[]): string[][] {
  const rows: string[][] = [];
  for (const node of nodes) {
    const indent = '  '.repeat(node.depth);
    rows.push([
      `${indent}${node.label}`,
      node.total.toFixed(2),
      node.count.toString(),
      node.avg.toFixed(2),
      `${node.percentage.toFixed(1)}%`,
    ]);
    if (node.children.length > 0) {
      rows.push(...flattenForCsv(node.children));
    }
  }
  return rows;
}

function exportPivotCsv() {
  const levels = groupByLevels.value.map(l => groupByOptions.find(o => o.value === l)?.title || l).join(' → ');
  const header = [`Group (${levels})`, 'Total Amount', 'Count', 'Avg Amount', '% of Total'];
  const dataRows = flattenForCsv(pivotTree.value);
  const footer = ['Grand Total', grandTotal.value.toFixed(2), totalCount.value.toString(), avgAmount.value.toFixed(2), '100%'];

  const csvContent = [header, ...dataRows, footer].map(row => row.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.setAttribute('href', URL.createObjectURL(blob));
  link.setAttribute('download', `expenditure-pivot-${groupByLevels.value.join('-')}-${new Date().toISOString().split('T')[0]}.csv`);
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
.pivot-row--root {
  background-color: #f8fafc;
}
.pivot-row--root:hover {
  background-color: #f1f5f9 !important;
}
</style>

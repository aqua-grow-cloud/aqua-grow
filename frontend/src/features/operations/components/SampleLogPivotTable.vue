<template>
  <v-card variant="outlined" class="rounded-lg shadow-sm">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b">
      <div class="d-flex align-center justify-space-between w-100 flex-wrap gap-2">
        <div class="d-flex align-center">
          <v-icon icon="mdi-chart-timeline-variant" color="primary" class="mr-2"></v-icon>
          Growth Analysis — Date × Species
        </div>
        <v-btn
          variant="outlined"
          prepend-icon="mdi-download"
          color="primary"
          density="compact"
          class="rounded-lg"
          @click="exportCsv"
        >
          Export
        </v-btn>
      </div>
    </v-card-title>

    <div v-if="isLoading" class="d-flex justify-center align-center py-12">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
    </div>

    <div v-else-if="rows.length === 0" class="text-center py-12 text-grey">
      <v-icon icon="mdi-table-off" size="48" class="mb-2 opacity-20"></v-icon>
      <div>No sample log records found.</div>
    </div>

    <div v-else class="overflow-x-auto">
      <table class="pivot-table w-100">
        <thead>
          <tr>
            <th class="col-date">Date</th>
            <th
              v-for="sp in allSpecies"
              :key="sp.id"
              class="col-species text-right"
            >
              {{ sp.name }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.date">
            <td class="col-date font-weight-medium">{{ formatDate(row.date) }}</td>
            <td
              v-for="sp in allSpecies"
              :key="sp.id"
              class="col-species text-right"
            >
              <template v-if="row.abwBySpecies[sp.id] != null">
                <span class="abw-value">{{ row.abwBySpecies[sp.id]!.toFixed(2) }} g</span>
                <span class="count-badge ml-1 text-caption text-grey">
                  ({{ row.countBySpecies[sp.id] }} pcs)
                </span>
              </template>
              <span v-else class="text-grey-lighten-2">—</span>
            </td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="footer-row">
            <td class="font-weight-bold">Latest ABW</td>
            <td
              v-for="sp in allSpecies"
              :key="sp.id"
              class="text-right font-weight-bold"
            >
              <span v-if="latestAbw[sp.id] != null">
                {{ latestAbw[sp.id]!.toFixed(2) }} g
              </span>
              <span v-else class="text-grey-lighten-2">—</span>
            </td>
          </tr>
          <tr class="footer-row">
            <td class="font-weight-bold">Growth Rate</td>
            <td
              v-for="sp in allSpecies"
              :key="sp.id"
              class="text-right"
            >
              <v-chip
                v-if="growthRates[sp.id] != null"
                size="x-small"
                :color="getGrowthColor(growthRates[sp.id]!)"
              >
                {{ growthRates[sp.id]! > 0 ? '+' : '' }}{{ growthRates[sp.id]!.toFixed(2) }} g/day
              </v-chip>
              <span v-else class="text-caption text-grey">N/A</span>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {type DateSpeciesPivotRow, sampleLogService} from '../api/sampleLogService';
import {fishTypeService} from '../../settings/api/fishTypeService';

const { data: pivotData, isLoading } = sampleLogService.useGetPivotByDate();
const { data: fishTypes } = fishTypeService.useGetAll();

// Sorted ascending by date (backend already sorts, but guard here too)
const rows = computed<DateSpeciesPivotRow[]>(() =>
  [...(pivotData.value ?? [])].sort((a, b) => a.date.localeCompare(b.date))
);

// Collect the full set of species that appear in any row, in a stable order
const allSpecies = computed(() => {
  const ids = new Set<string>();
  rows.value.forEach(r => Object.keys(r.abwBySpecies).forEach(id => ids.add(id)));

  return Array.from(ids).map(id => ({
    id,
    name: (fishTypes.value as any)?.find((t: any) => t.id === id)?.name ?? id,
  })).sort((a, b) => a.name.localeCompare(b.name));
});

// Latest ABW per species (from the most recent date that has data for that species)
const latestAbw = computed<Record<string, number>>(() => {
  const result: Record<string, number> = {};
  // rows are ascending by date, so iterate forward and keep overwriting
  rows.value.forEach(r => {
    Object.entries(r.abwBySpecies).forEach(([sp, abw]) => {
      result[sp] = abw;
    });
  });
  return result;
});

// Growth rate per species: (latestABW - firstABW) / days between first and last sample date for that species
const growthRates = computed<Record<string, number | null>>(() => {
  const first: Record<string, { date: string; abw: number }> = {};
  const last: Record<string, { date: string; abw: number }> = {};

  rows.value.forEach(r => {
    Object.entries(r.abwBySpecies).forEach(([sp, abw]) => {
      if (!first[sp]) first[sp] = { date: r.date, abw };
      last[sp] = { date: r.date, abw };
    });
  });

  const result: Record<string, number | null> = {};
  allSpecies.value.forEach(({ id }) => {
    const f = first[id];
    const l = last[id];
    if (!f || !l || f.date === l.date) {
      result[id] = null;
      return;
    }
    const days = Math.floor(
      (new Date(l.date).getTime() - new Date(f.date).getTime()) / 86_400_000
    );
    result[id] = days > 0 ? (l.abw - f.abw) / days : null;
  });
  return result;
});

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  });
}

function getGrowthColor(rate: number): string {
  if (rate >= 1.0) return 'success';
  if (rate >= 0.5) return 'info';
  if (rate >= 0) return 'warning';
  return 'error';
}

function exportCsv() {
  const speciesCols = allSpecies.value.map(s => s.name);
  const header = ['Date', ...speciesCols.flatMap(n => [`${n} ABW (g)`, `${n} Count`])];
  const dataRows = rows.value.map(r => [
    r.date,
    ...allSpecies.value.flatMap(({ id }) => [
      r.abwBySpecies[id] != null ? r.abwBySpecies[id]!.toFixed(2) : '',
      r.countBySpecies[id] != null ? r.countBySpecies[id].toString() : '',
    ]),
  ]);
  const latestRow = [
    'Latest ABW',
    ...allSpecies.value.flatMap(({ id }) => [
      latestAbw.value[id] != null ? latestAbw.value[id]!.toFixed(2) : '',
      '',
    ]),
  ];
  const growthRow = [
    'Growth Rate (g/day)',
    ...allSpecies.value.flatMap(({ id }) => [
      growthRates.value[id] != null ? growthRates.value[id]!.toFixed(2) : 'N/A',
      '',
    ]),
  ];

  const csv = [header, ...dataRows, latestRow, growthRow]
    .map(r => r.map(c => `"${c}"`).join(','))
    .join('\n');

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.setAttribute('href', URL.createObjectURL(blob));
  link.setAttribute('download', `growth-analysis-${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
</script>

<style scoped>
.pivot-table {
  border-collapse: collapse;
  font-size: 0.875rem;
}

.pivot-table th,
.pivot-table td {
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
  vertical-align: middle;
}

.pivot-table thead th {
  background: #f8fafc;
  font-weight: 600;
  font-size: 0.8125rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #475569;
}

.pivot-table tbody tr:hover td {
  background: #f1f5f9;
}

.col-date {
  min-width: 140px;
  position: sticky;
  left: 0;
  background: inherit;
  z-index: 1;
}

.col-species {
  min-width: 140px;
}

.footer-row td {
  background: #f1f5f9;
  border-top: 2px solid #cbd5e1;
}

.abw-value {
  font-weight: 500;
}

.count-badge {
  opacity: 0.7;
}
</style>

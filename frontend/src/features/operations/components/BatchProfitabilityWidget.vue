<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center">
      <v-icon icon="mdi-cash-multiple" color="green" class="mr-2"></v-icon>
      Batch Profitability
    </v-card-title>

    <div v-if="isLoading" class="d-flex justify-center py-8">
      <v-progress-circular indeterminate color="green"></v-progress-circular>
    </div>

    <div v-else-if="rows.length === 0" class="text-center py-10 text-grey">
      <v-icon icon="mdi-cash-off" size="40" class="mb-2 opacity-30"></v-icon>
      <div>No financial data available</div>
    </div>

    <template v-else>
      <v-chart :option="chartOption" autoresize style="height: 240px"></v-chart>
      <v-divider></v-divider>
      <v-table density="compact" hover>
        <thead>
          <tr class="bg-grey-lighten-4">
            <th class="text-subtitle-2 font-weight-bold">Batch</th>
            <th class="text-subtitle-2 font-weight-bold text-right">Revenue</th>
            <th class="text-subtitle-2 font-weight-bold text-right">Spend</th>
            <th class="text-subtitle-2 font-weight-bold text-right">Net Profit</th>
            <th class="text-subtitle-2 font-weight-bold text-right">ROI</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.batchId">
            <td class="font-weight-medium">{{ row.batchName }}</td>
            <td class="text-right text-success">₹{{ fmt(row.revenue) }}</td>
            <td class="text-right text-error">₹{{ fmt(row.spend) }}</td>
            <td class="text-right font-weight-bold" :class="row.net >= 0 ? 'text-success' : 'text-error'">
              {{ row.net >= 0 ? '+' : '' }}₹{{ fmt(row.net) }}
            </td>
            <td class="text-right">
              <v-chip :color="row.roi >= 0 ? 'success' : 'error'" size="x-small" variant="tonal">
                {{ row.roi >= 0 ? '+' : '' }}{{ row.roi.toFixed(1) }}%
              </v-chip>
            </td>
          </tr>
        </tbody>
      </v-table>
    </template>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {use} from 'echarts/core';
import {CanvasRenderer} from 'echarts/renderers';
import {BarChart} from 'echarts/charts';
import {GridComponent, LegendComponent, TooltipComponent} from 'echarts/components';
import VChart from 'vue-echarts';
import {useBatches} from '../../batches/composables/useBatches';
import type {Expenditure, HarvestLog} from '@/api/generated/model';

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent]);

const props = defineProps<{
  expenditures: Expenditure[];
  harvestLogs: HarvestLog[];
  isLoading?: boolean;
}>();

const { batches } = useBatches();

const completedBatches = computed(() =>
  batches.value?.filter(b => b.status === 'Completed') || []
);

const rows = computed(() => {
  return completedBatches.value.map(batch => {
    const revenue = props.harvestLogs
      .filter(l => l.batchId === batch.id)
      .reduce((s, l) => s + (l.revenue || 0), 0);

    const spend = props.expenditures
      .filter(e => e.batchId === batch.id)
      .reduce((s, e) => s + (e.amount || 0), 0);

    const net = revenue - spend;
    const roi = spend > 0 ? (net / spend) * 100 : 0;

    return {
      batchId: batch.id!,
      batchName: batch.name || batch.stockingDate || batch.id!,
      revenue,
      spend,
      net,
      roi,
    };
  })
    .filter(r => r.revenue > 0 || r.spend > 0)
    .sort((a, b) => b.net - a.net);
});

function fmt(val: number): string {
  return Math.abs(val).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 60, right: 20, top: 36, bottom: 50 },
  xAxis: {
    type: 'category',
    data: rows.value.map(r => r.batchName),
    axisLabel: { rotate: 20, fontSize: 11 },
  },
  yAxis: { type: 'value', axisLabel: { formatter: (v: number) => '₹' + (v / 1000).toFixed(0) + 'k' } },
  series: [
    {
      name: 'Revenue',
      type: 'bar',
      data: rows.value.map(r => r.revenue),
      itemStyle: { color: '#22c55e' },
    },
    {
      name: 'Spend',
      type: 'bar',
      data: rows.value.map(r => r.spend),
      itemStyle: { color: '#ef4444' },
    },
    {
      name: 'Net Profit',
      type: 'bar',
      data: rows.value.map(r => r.net),
      itemStyle: {
        color: (p: any) => p.value >= 0 ? '#3b82f6' : '#f97316',
      },
    },
  ],
}));
</script>

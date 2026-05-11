<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center justify-space-between">
      <div class="d-flex align-center">
        <v-icon icon="mdi-chart-bar" color="indigo" class="mr-2"></v-icon>
        Monthly Revenue vs Expenditure
      </div>
      <v-select
        v-model="months"
        :items="[3, 6, 12, 24]"
        label="Months"
        density="compact"
        variant="outlined"
        hide-details
        style="max-width:100px"
      ></v-select>
    </v-card-title>

    <!-- Summary chips -->
    <div class="px-6 pt-3 d-flex gap-3 flex-wrap">
      <v-chip color="success" size="small" variant="tonal" prepend-icon="mdi-trending-up">
        Revenue: ₹{{ totalRevenue.toLocaleString('en-IN') }}
      </v-chip>
      <v-chip color="error" size="small" variant="tonal" prepend-icon="mdi-trending-down">
        Spend: ₹{{ totalSpend.toLocaleString('en-IN') }}
      </v-chip>
      <v-chip :color="netPosition >= 0 ? 'primary' : 'error'" size="small" variant="tonal" prepend-icon="mdi-scale-balance">
        Net: {{ netPosition >= 0 ? '+' : '' }}₹{{ netPosition.toLocaleString('en-IN') }}
      </v-chip>
    </div>

    <div v-if="chartData.labels.length === 0" class="text-center py-10 text-grey">
      <v-icon icon="mdi-chart-bar-stacked" size="40" class="mb-2 opacity-30"></v-icon>
      <div>No financial data available</div>
    </div>

    <v-chart v-else :option="chartOption" autoresize style="height: 300px"></v-chart>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {use} from 'echarts/core';
import {CanvasRenderer} from 'echarts/renderers';
import {BarChart, LineChart} from 'echarts/charts';
import {GridComponent, LegendComponent, TooltipComponent} from 'echarts/components';
import VChart from 'vue-echarts';
import type {Expenditure, HarvestLog} from '@/api/generated/model';

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent]);

const props = defineProps<{
  expenditures: Expenditure[];
  harvestLogs: HarvestLog[];
}>();

const months = ref(12);

function toMonthKey(dateStr?: string): string {
  if (!dateStr) return '';
  return dateStr.slice(0, 7); // 'YYYY-MM'
}

function formatMonthLabel(key: string): string {
  if (!key) return '';
  const [y, m] = key.split('-');
  return new Date(Number(y), Number(m) - 1).toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
}

const chartData = computed(() => {
  // Build rolling window of N months
  const now = new Date();
  const keys: string[] = [];
  for (let i = months.value - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  const revenueByMonth: Record<string, number> = {};
  const spendByMonth: Record<string, number> = {};
  keys.forEach(k => { revenueByMonth[k] = 0; spendByMonth[k] = 0; });

  props.harvestLogs.forEach(l => {
    const k = toMonthKey(l.date);
    if (k in revenueByMonth) revenueByMonth[k] = (revenueByMonth[k] ?? 0) + (l.revenue ?? 0);
  });

  props.expenditures.forEach(e => {
    const k = toMonthKey(e.date);
    if (k in spendByMonth) spendByMonth[k] = (spendByMonth[k] ?? 0) + (e.amount ?? 0);
  });

  return {
    labels: keys.map(formatMonthLabel),
    revenue: keys.map(k => Math.round(revenueByMonth[k] ?? 0)),
    spend: keys.map(k => Math.round(spendByMonth[k] ?? 0)),
    net: keys.map(k => Math.round((revenueByMonth[k] ?? 0) - (spendByMonth[k] ?? 0))),
  };
});

const totalRevenue = computed(() => chartData.value.revenue.reduce((a, b) => a + b, 0));
const totalSpend = computed(() => chartData.value.spend.reduce((a, b) => a + b, 0));
const netPosition = computed(() => totalRevenue.value - totalSpend.value);

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: (params: any[]) =>
      params.map((p: any) => `${p.seriesName}: ₹${Number(p.value).toLocaleString('en-IN')}`).join('<br>'),
  },
  legend: { top: 0 },
  grid: { left: 60, right: 20, top: 36, bottom: 40 },
  xAxis: {
    type: 'category',
    data: chartData.value.labels,
    axisLabel: { fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    axisLabel: { formatter: (v: number) => '₹' + (v >= 1000 ? (v / 1000).toFixed(0) + 'k' : v) },
  },
  series: [
    {
      name: 'Revenue',
      type: 'bar',
      data: chartData.value.revenue,
      itemStyle: { color: '#22c55e', borderRadius: [3, 3, 0, 0] },
    },
    {
      name: 'Spend',
      type: 'bar',
      data: chartData.value.spend,
      itemStyle: { color: '#ef4444', borderRadius: [3, 3, 0, 0] },
    },
    {
      name: 'Net',
      type: 'line',
      data: chartData.value.net,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: '#6366f1' },
      itemStyle: { color: '#6366f1' },
    },
  ],
}));
</script>

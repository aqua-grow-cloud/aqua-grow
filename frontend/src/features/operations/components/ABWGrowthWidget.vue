<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-2">
      <div class="d-flex align-center">
        <v-icon icon="mdi-chart-line" color="green" class="mr-2"></v-icon>
        ABW Growth Curves
      </div>
      <v-select
        v-model="selectedBatchIds"
        :items="batchOptions"
        item-title="label"
        item-value="id"
        label="Batches"
        density="compact"
        variant="outlined"
        hide-details
        multiple
        chips
        closable-chips
        class="w-64"
        style="max-width:320px"
      ></v-select>
    </v-card-title>

    <div v-if="selectedBatchIds.length === 0" class="text-center py-10 text-grey">
      <v-icon icon="mdi-chart-line-variant" size="40" class="mb-2 opacity-30"></v-icon>
      <div>Select batches to compare growth</div>
    </div>

    <div v-else-if="noData" class="text-center py-10 text-grey">
      <v-icon icon="mdi-flask-empty" size="40" class="mb-2 opacity-30"></v-icon>
      <div>No sample data for selected batches</div>
    </div>

    <v-chart
      v-else
      :option="chartOption"
      autoresize
      style="height: 320px"
    ></v-chart>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {use} from 'echarts/core';
import {CanvasRenderer} from 'echarts/renderers';
import {LineChart} from 'echarts/charts';
import {GridComponent, LegendComponent, TooltipComponent} from 'echarts/components';
import VChart from 'vue-echarts';
import {useBatches} from '../../batches/composables/useBatches';
import type {SampleLog} from '@/api/generated/model';

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent]);

const props = defineProps<{
  sampleLogs: SampleLog[];
}>();

const { batches } = useBatches();

// Include all batches that have at least one sample log
const batchesWithSamples = computed(() => {
  const batchIdsWithData = new Set(props.sampleLogs.map(s => s.batchId).filter(Boolean));
  return (batches.value || []).filter(b => batchIdsWithData.has(b.id));
});

const batchOptions = computed(() =>
  batchesWithSamples.value.map(b => ({ id: b.id!, label: b.name || b.stockingDate || b.id! }))
);

const selectedBatchIds = ref<string[]>([]);

// Auto-select first 3 batches when options load
watch(batchOptions, (opts) => {
  if (selectedBatchIds.value.length === 0 && opts.length > 0) {
    selectedBatchIds.value = opts.slice(0, 3).map(o => o.id);
  }
}, { immediate: true });

const COLORS = ['#22c55e', '#3b82f6', '#f59e0b', '#8b5cf6', '#ef4444', '#14b8a6'];

// Build one series per selected batch using DOC (days of culture) as x-axis
const seriesData = computed(() => {
  return selectedBatchIds.value.map((batchId, idx) => {
    const batch = batchesWithSamples.value.find(b => b.id === batchId);
    const stockingDate = batch?.stockingDate ? new Date(batch.stockingDate) : null;

    const samples = props.sampleLogs
      .filter(s => s.batchId === batchId && s.averageWeight != null)
      .sort((a, b) => (a.date || '').localeCompare(b.date || ''));

    // Group by date, average weight per day
    const byDate: Record<string, number[]> = {};
    samples.forEach(s => {
      const d = s.date || '';
      if (!byDate[d]) byDate[d] = [];
      if (s.averageWeight != null) byDate[d].push(s.averageWeight);
    });

    const points: [number, number][] = Object.entries(byDate).map(([date, weights]) => {
      const doc = stockingDate
        ? Math.floor((new Date(date).getTime() - stockingDate.getTime()) / 86_400_000)
        : 0;
      const avg = weights.reduce((a: number, b: number) => a + b, 0) / weights.length;
      return [doc, parseFloat(avg.toFixed(2))] as [number, number];
    }).sort((a, b) => a[0] - b[0]);

    return {
      name: batch?.name || batchId,
      type: 'line',
      data: points,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: COLORS[idx % COLORS.length] },
      itemStyle: { color: COLORS[idx % COLORS.length] },
    };
  });
});

const noData = computed(() => seriesData.value.every(s => s.data.length === 0));

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: (params: any[]) =>
      params.map((p: any) => `${p.seriesName}: <b>${p.value[1]} g</b> (DOC ${p.value[0]})`).join('<br>'),
  },
  legend: { top: 0, type: 'scroll' },
  grid: { left: 55, right: 20, top: 40, bottom: 40 },
  xAxis: {
    type: 'value',
    name: 'Days of Culture',
    nameTextStyle: { fontSize: 11 },
    axisLabel: { fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    name: 'ABW (g)',
    nameTextStyle: { fontSize: 11 },
  },
  series: seriesData.value,
}));
</script>

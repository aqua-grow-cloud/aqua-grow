<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center justify-space-between">
      <div class="d-flex align-center">
        <v-icon icon="mdi-water" color="blue" class="mr-2"></v-icon>
        Water Quality Trends
      </div>
      <div class="d-flex gap-2 align-center">
        <v-select
          v-model="selectedBatchId"
          :items="batchOptions"
          item-title="label"
          item-value="id"
          label="Batch"
          density="compact"
          variant="outlined"
          hide-details
          class="w-48"
        ></v-select>
        <v-btn-toggle v-model="selectedMetric" density="compact" variant="outlined" color="primary" mandatory>
          <v-btn value="doLevel" size="small">DO</v-btn>
          <v-btn value="ph" size="small">pH</v-btn>
          <v-btn value="temperature" size="small">Temp</v-btn>
          <v-btn value="ammonia" size="small">NH₃</v-btn>
        </v-btn-toggle>
      </div>
    </v-card-title>

    <!-- Latest Reading Chips -->
    <div v-if="latestReading" class="px-6 pt-4 d-flex gap-3 flex-wrap">
      <v-chip
        v-for="m in metrics"
        :key="m.key"
        :color="getStatusColor(m.key, latestReading[m.key])"
        size="small"
        variant="tonal"
        :prepend-icon="m.icon"
      >
        {{ m.label }}: {{ latestReading[m.key] != null ? Number(latestReading[m.key]).toFixed(1) : '—' }} {{ m.unit }}
      </v-chip>
      <v-chip size="small" variant="tonal" color="grey" prepend-icon="mdi-calendar">
        {{ latestReading.date }}
      </v-chip>
    </div>

    <div v-if="isFetching" class="d-flex justify-center py-8">
      <v-progress-circular indeterminate color="blue"></v-progress-circular>
    </div>

    <div v-else-if="!selectedBatchId" class="text-center py-10 text-grey">
      <v-icon icon="mdi-water-off" size="40" class="mb-2 opacity-30"></v-icon>
      <div>Select a batch to view water quality trends</div>
    </div>

    <div v-else-if="chartLogs.length === 0" class="text-center py-10 text-grey">
      <v-icon icon="mdi-water-off" size="40" class="mb-2 opacity-30"></v-icon>
      <div>No monitoring data recorded for this batch</div>
    </div>

    <v-chart
      v-else
      :option="chartOption"
      autoresize
      style="height: 280px"
      class="px-2"
    ></v-chart>

    <!-- Thresholds legend -->
    <div class="px-6 pb-4 d-flex gap-4 flex-wrap">
      <div v-for="t in currentThresholds" :key="t.label" class="d-flex align-center gap-1 text-caption">
        <span :class="`bg-${t.color}`" style="width:10px;height:10px;border-radius:50%;display:inline-block;opacity:0.7"></span>
        {{ t.label }}
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {use} from 'echarts/core';
import {CanvasRenderer} from 'echarts/renderers';
import {LineChart} from 'echarts/charts';
import {GridComponent, LegendComponent, MarkLineComponent, TooltipComponent} from 'echarts/components';
import VChart from 'vue-echarts';
import {useGetMonitoringLogsByBatch} from '@/api/generated/monitoring-log-controller/monitoring-log-controller';
import {useBatches} from '../../batches/composables/useBatches';
import type {MonitoringLog} from '@/api/generated/model';

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent]);

const { batches } = useBatches();

const activeBatches = computed(() =>
  batches.value?.filter(b => b.status === 'Active') || []
);

const batchOptions = computed(() =>
  activeBatches.value.map(b => ({ id: b.id!, label: b.name || b.stockingDate || b.id! }))
);

const selectedBatchId = ref<string>(activeBatches.value[0]?.id || '');
const selectedMetric = ref<keyof MonitoringLog>('doLevel');

const { data: rawLogs, isFetching } = useGetMonitoringLogsByBatch(selectedBatchId);

const chartLogs = computed<MonitoringLog[]>(() => {
  if (!rawLogs.value) return [];
  return [...rawLogs.value].sort((a, b) => (a.date || '').localeCompare(b.date || ''));
});

const latestReading = computed(() => {
  if (!chartLogs.value.length) return null;
  return chartLogs.value[chartLogs.value.length - 1] as MonitoringLog;
});

const metrics = [
  { key: 'doLevel' as keyof MonitoringLog, label: 'DO', unit: 'mg/L', icon: 'mdi-molecule' },
  { key: 'ph' as keyof MonitoringLog, label: 'pH', unit: '', icon: 'mdi-flask' },
  { key: 'temperature' as keyof MonitoringLog, label: 'Temp', unit: '°C', icon: 'mdi-thermometer' },
  { key: 'ammonia' as keyof MonitoringLog, label: 'NH₃', unit: 'ppm', icon: 'mdi-chemical-weapon' },
];

const thresholds: Record<string, { min?: number; max?: number; ideal?: number }> = {
  doLevel: { min: 5 },
  ph: { min: 7.5, max: 8.5 },
  temperature: { min: 26, max: 32 },
  ammonia: { max: 0.5 },
};

const currentThresholds = computed(() => {
  const t = thresholds[selectedMetric.value as string] || {};
  const result = [];
  if (t.min != null) result.push({ label: `Min ${t.min}`, color: 'warning' });
  if (t.max != null) result.push({ label: `Max ${t.max}`, color: 'error' });
  return result;
});

const metricConfig = computed(() => metrics.find(m => m.key === selectedMetric.value));

function getStatusColor(key: keyof MonitoringLog, val?: string | number | boolean | null): string {
  const num = typeof val === 'number' ? val : null;
  if (num == null) return 'grey';
  const t = thresholds[key as string];
  if (!t) return 'primary';
  if (t.min != null && num < t.min) return 'error';
  if (t.max != null && num > t.max) return 'error';
  return 'success';
}

const chartOption = computed(() => {
  const dates = chartLogs.value.map(l => l.date || '');
  const values = chartLogs.value.map(l => {
    const v = l[selectedMetric.value];
    return typeof v === 'number' ? Number(v.toFixed(2)) : null;
  });

  const t = thresholds[selectedMetric.value as string] || {};
  const markLines: any[] = [];
  if (t.min != null) markLines.push({ yAxis: t.min, name: 'Min', lineStyle: { color: '#f59e0b', type: 'dashed' } });
  if (t.max != null) markLines.push({ yAxis: t.max, name: 'Max', lineStyle: { color: '#ef4444', type: 'dashed' } });

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: metricConfig.value?.unit || '',
      nameTextStyle: { fontSize: 11 },
    },
    series: [{
      name: metricConfig.value?.label || '',
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: '#3b82f6' },
      itemStyle: { color: '#3b82f6' },
      areaStyle: { color: 'rgba(59,130,246,0.08)' },
      markLine: markLines.length ? { silent: true, data: markLines } : undefined,
    }],
  };
});
</script>

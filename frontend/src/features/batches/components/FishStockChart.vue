<template>
  <v-card class="stock-chart-card overflow-hidden" elevation="2">
    <v-card-title class="d-flex align-center py-3 px-4">
      <v-icon color="primary" class="mr-2">mdi-chart-line</v-icon>
      <span class="text-h6 font-weight-bold">Fish Stock Analysis</span>
      <v-spacer></v-spacer>
      <v-btn-toggle
        v-model="timeRange"
        mandatory
        density="compact"
        color="primary"
        variant="outlined"
      >
        <v-btn value="30" size="small">30D</v-btn>
        <v-btn value="90" size="small">90D</v-btn>
        <v-btn value="all" size="small">ALL</v-btn>
      </v-btn-toggle>
    </v-card-title>

    <v-divider></v-divider>

    <v-card-text class="pa-0">
      <div v-if="loading" class="d-flex align-center justify-center" style="height: 400px">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
      </div>
      <div v-else-if="!chartData || chartData.length === 0" class="d-flex flex-column align-center justify-center text-grey" style="height: 400px">
        <v-icon size="64" class="mb-2">mdi-database-off</v-icon>
        <p>No stock events recorded yet</p>
      </div>
      <v-chart
        v-else
        class="chart"
        :option="chartOption"
        autoresize
        style="height: 400px"
      />
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';
import { FishStockEventType, type FishStockEvent } from '@/api/manual/types';
import type { FishType } from '../../settings/api/fishTypeService';

// Register ECharts modules
use([
  CanvasRenderer,
  BarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
]);

const props = defineProps<{
  events: FishStockEvent[];
  fishTypes: FishType[];
  loading?: boolean;
}>();

const timeRange = ref('all');
const COLORS = ['#1867C0', '#4CAF50', '#FB8C00', '#F44336', '#9C27B0', '#00BCD4', '#FFEB3B', '#795548'];

const chartData = computed(() => {
  if (!props.events || props.events.length === 0) return [];

  // Sort events by date
  const sorted = [...props.events].sort((a, b) => 
    new Date(a.date).getTime() - new Date(b.date).getTime()
  );

  // Track state per species
  const speciesState = new Map<string, { quantity: number, lastABW: number }>();
  
  // Group by date
  const dailyData = new Map<string, {
    seeds: number,
    harvests: number,
    mortality: number,
    remaining: number,
    biomassByType: Record<string, number>,
    totalBiomass: number
  }>();

  let cumulativeTotalStock = 0;
  
  sorted.forEach(event => {
    const date = event.date;
    const typeId = event.fishTypeId || 'unknown';

    if (!dailyData.has(date)) {
      dailyData.set(date, { 
        seeds: 0, 
        harvests: 0, 
        mortality: 0, 
        remaining: 0, 
        biomassByType: {}, 
        totalBiomass: 0 
      });
    }
    
    if (!speciesState.has(typeId)) {
      speciesState.set(typeId, { quantity: 0, lastABW: 0 });
    }

    const day = dailyData.get(date)!;
    const state = speciesState.get(typeId)!;
    
    // Update quantity deltas for the day (for bar charts)
    if (event.type === FishStockEventType.SEED || event.type === FishStockEventType.TRANSFER_IN) {
      day.seeds += event.quantityDelta;
    } else if (event.type === FishStockEventType.HARVEST) {
      day.harvests += Math.abs(event.quantityDelta);
    } else if (event.type === FishStockEventType.MORTALITY) {
      day.mortality += Math.abs(event.quantityDelta);
    } else if (event.type === FishStockEventType.TRANSFER_OUT) {
      day.mortality += Math.abs(event.quantityDelta);
    }

    // Update cumulative state
    state.quantity += event.quantityDelta;
    cumulativeTotalStock += event.quantityDelta;
    
    if (event.avgWeightGram) {
      state.lastABW = event.avgWeightGram;
    }

    // Capture snapshot for all species on this date
    let currentTotalBiomass = 0;
    speciesState.forEach((s, tid) => {
       const b = (s.quantity * s.lastABW) / 1000;
       day.biomassByType[tid] = b;
       currentTotalBiomass += b;
    });

    day.remaining = cumulativeTotalStock;
    day.totalBiomass = currentTotalBiomass;
  });

  return Array.from(dailyData.entries()).map(([date, data]) => ({
    date,
    ...data
  }));
});

const chartOption = computed(() => {
  const data = chartData.value;
  const dates = data.map(d => d.date);
  
  // Identify all species found in the data
  const speciesInChart = new Set<string>();
  data.forEach(d => Object.keys(d.biomassByType).forEach(id => speciesInChart.add(id)));
  const sortedSpeciesIds = Array.from(speciesInChart).sort();

  const series: any[] = [
    {
      name: 'Stocking',
      type: 'bar',
      barMaxWidth: 20,
      itemStyle: { color: '#4CAF50', borderRadius: [4, 4, 0, 0] },
      data: data.map(d => d.seeds)
    },
    {
      name: 'Harvest',
      type: 'bar',
      barMaxWidth: 20,
      itemStyle: { color: '#FF9800', borderRadius: [4, 4, 0, 0] },
      data: data.map(d => d.harvests)
    },
    {
      name: 'Remaining Stock',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#2196F3' },
      itemStyle: { color: '#2196F3' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(33, 150, 243, 0.2)' }, { offset: 1, color: 'rgba(33, 150, 243, 0)' }]
        }
      },
      data: data.map(d => d.remaining)
    },
    {
      name: 'Total Biomass',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      lineStyle: { width: 4, color: '#333' },
      itemStyle: { color: '#333' },
      data: data.map(d => d.totalBiomass),
      z: 10 // Ensure total is on top
    }
  ];

  // Add individual species biomass series
  sortedSpeciesIds.forEach((id, index) => {
    const speciesName = props.fishTypes.find(t => t.id === id)?.name || id;
    series.push({
      name: `Biomass (${speciesName})`,
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      lineStyle: { width: 2, type: 'dashed' },
      itemStyle: { color: COLORS[index % COLORS.length] },
      data: data.map(d => d.biomassByType[id] || 0)
    });
  });

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderWidth: 1,
      borderColor: '#eee',
      textStyle: { color: '#333' },
      formatter: (params: any) => {
        let res = `<div class="font-weight-bold mb-1">${params[0].name}</div>`;
        params.forEach((p: any) => {
          const val = (p.value || 0).toLocaleString(undefined, { minimumFractionDigits: p.seriesName.includes('Biomass') ? 1 : 0 });
          const unit = p.seriesName.includes('Biomass') ? ' kg' : ' pcs';
          res += `<div class="d-flex align-center">
            <span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${p.color};"></span>
            <span class="mr-4">${p.seriesName}:</span>
            <span class="ml-auto font-weight-bold">${val}${unit}</span>
          </div>`;
        });
        return res;
      }
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 10,
      icon: 'circle',
      padding: [0, 50]
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Quantity (pcs)',
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } }
      },
      {
        type: 'value',
        name: 'Biomass (kg)',
        position: 'right',
        splitLine: { show: false }
      }
    ],
    series
  };
});
</script>

<style scoped>
.stock-chart-card {
  border-radius: 12px;
}
.chart {
  width: 100%;
}
</style>

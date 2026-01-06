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
  loading?: boolean;
}>();

const timeRange = ref('all');

const chartData = computed(() => {
  if (!props.events || props.events.length === 0) return [];

  // Sort events by date
  const sorted = [...props.events].sort((a, b) => 
    new Date(a.date).getTime() - new Date(b.date).getTime()
  );

  // Group by date
  const dailyData = new Map<string, {
    seeds: number,
    harvests: number,
    mortality: number,
    remaining: number,
    biomass: number
  }>();

  let cumulativeStock = 0;
  
  sorted.forEach(event => {
    const date = event.date;
    if (!dailyData.has(date)) {
      dailyData.set(date, { seeds: 0, harvests: 0, mortality: 0, remaining: 0, biomass: 0 });
    }
    
    const day = dailyData.get(date)!;
    
    if (event.type === FishStockEventType.SEED || event.type === FishStockEventType.TRANSFER_IN) {
      day.seeds += event.quantityDelta;
    } else if (event.type === FishStockEventType.HARVEST) {
      day.harvests += Math.abs(event.quantityDelta);
    } else if (event.type === FishStockEventType.MORTALITY) {
      day.mortality += Math.abs(event.quantityDelta);
    } else if (event.type === FishStockEventType.TRANSFER_OUT) {
      // For simplicity, treat transfer out like a removal
      day.mortality += Math.abs(event.quantityDelta);
    }

    cumulativeStock += event.quantityDelta;
    day.remaining = cumulativeStock;
    
    // Calculate biomass if avgWeight is available
    if (event.avgWeightGram) {
       day.biomass = (cumulativeStock * event.avgWeightGram) / 1000; // in kg
    }
  });

  return Array.from(dailyData.entries()).map(([date, data]) => ({
    date,
    ...data
  }));
});

const chartOption = computed(() => {
  const data = chartData.value;
  const dates = data.map(d => d.date);

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
          const val = p.value.toLocaleString();
          const unit = p.seriesName === 'Biomass' ? ' kg' : ' pcs';
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
      data: ['Stocking', 'Harvest', 'Remaining Stock', 'Biomass'],
      bottom: 10,
      icon: 'circle'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
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
    series: [
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
        name: 'Biomass',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        lineStyle: { width: 2, type: 'dashed', color: '#9C27B0' },
        itemStyle: { color: '#9C27B0' },
        data: data.map(d => d.biomass)
      }
    ]
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

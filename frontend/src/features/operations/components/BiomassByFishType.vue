<template>
  <v-card variant="outlined" class="rounded-lg h-100">
    <v-card-title class="px-6 py-4 d-flex align-center">
      <v-icon icon="mdi-chart-pie" class="mr-2" color="primary"></v-icon>
      <span>Biomass Distribution</span>
    </v-card-title>
    <v-divider></v-divider>

    <v-card-text>
      <v-row>
        <v-col cols="12" md="6">
          <div v-if="chartData.length > 0" class="chart-container">
            <v-chart class="chart" :option="chartOption" autoresize />
          </div>
          <div v-else class="d-flex flex-column align-center justify-center text-grey h-100 py-8">
            <v-icon icon="mdi-chart-arc" size="48" class="mb-2 opacity-20"></v-icon>
            <div>No biomass data available</div>
          </div>
        </v-col>

        <v-col cols="12" md="6">
          <v-table density="compact">
            <thead>
              <tr>
                <th class="text-left font-weight-bold">Fish Type</th>
                <th class="text-right font-weight-bold">ABW (g)</th>
                <th class="text-right font-weight-bold">Biomass (kg)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in summaryData" :key="item.id">
                <td>
                  <v-icon icon="mdi-circle" size="small" :color="item.color" class="mr-1"></v-icon>
                  {{ item.name }}
                </td>
                <td class="text-right">{{ item.avgWeight.toFixed(1) }}</td>
                <td class="text-right font-weight-bold">{{ item.biomass.toLocaleString() }}</td>
              </tr>
              <tr v-if="summaryData.length > 0" class="bg-grey-lighten-4">
                <td colspan="2" class="font-weight-bold">Total</td>
                <td class="text-right font-weight-bold">{{ totalBiomass.toLocaleString() }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {use} from 'echarts/core';
import {CanvasRenderer} from 'echarts/renderers';
import {PieChart} from 'echarts/charts';
import {LegendComponent, TitleComponent, TooltipComponent,} from 'echarts/components';
import VChart from 'vue-echarts';
import type {Batch, HarvestLog, SampleLog} from '../../../api/generated/model';
import type {FishType} from '../../settings/api/fishTypeService';

use([
  CanvasRenderer,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
]);

const props = defineProps<{
  batches: Batch[];
  fishTypes: FishType[];
  harvestLogs: HarvestLog[];
  sampleLogs: SampleLog[];
}>();

const COLORS = ['#1867C0', '#4CAF50', '#FB8C00', '#F44336', '#9C27B0', '#00BCD4', '#FFEB3B', '#795548'];

const biomassByType = computed(() => {
  const result = new Map<string, { biomass: number, quantity: number, totalWeight: number, count: number }>();

  props.batches.forEach(batch => {
    if (batch.status !== 'Active') return;

    batch.fishSeeds?.forEach(seed => {
      const typeId = seed.fishTypeId;
      if (!typeId) return;

      if (!result.has(typeId)) {
        result.set(typeId, { biomass: 0, quantity: 0, totalWeight: 0, count: 0 });
      }

      const stats = result.get(typeId)!;

      // Calculate current quantity for this type in this batch
      const totalStocked = seed.quantity || 0;
      const totalHarvested = props.harvestLogs
        .filter(h => h.batchId === batch.id && h.fishTypeId === typeId)
        .reduce((sum, h) => sum + (h.count || 0), 0);

      let currentQuantity = Math.max(0, totalStocked - totalHarvested);

      // Get latest sampling for this batch and type with same-day averaging
      const samplesForBatchType = props.sampleLogs
        .filter(s => s.batchId === batch.id && s.fishTypeId === typeId && s.averageWeight && s.averageWeight > 0)
        .sort((a, b) => new Date(b.date || '').getTime() - new Date(a.date || '').getTime());

      // Calculate average ABW from same-day samples (most recent date)
      let abw = seed.sizeGrams || 0; // Default to seed size
      let latestSurvivalEstimate: number | undefined;

      if (samplesForBatchType.length > 0) {
        const mostRecentDate = samplesForBatchType[0]?.date;
        const sameDaySamples = samplesForBatchType.filter(s => s.date === mostRecentDate);

        // Average ABW from all samples on the most recent date
        const totalWeight = sameDaySamples.reduce((sum, s) => sum + (s.averageWeight || 0), 0);
        abw = totalWeight / sameDaySamples.length;

        // Use survival estimate from the most recent sample (if available)
        latestSurvivalEstimate = sameDaySamples[0]?.survivalEstimate;
      }

      if (latestSurvivalEstimate !== undefined) {
        currentQuantity = currentQuantity * (latestSurvivalEstimate / 100);
      }

      const biomass = (currentQuantity * abw) / 1000;

      stats.biomass += biomass;
      stats.quantity += currentQuantity;
      stats.totalWeight += (currentQuantity * abw);
      stats.count++;
    });
  });

  return result;
});

const summaryData = computed(() => {
  return Array.from(biomassByType.value.entries())
    .map(([typeId, stats], index) => {
      const type = props.fishTypes.find(t => t.id === typeId);
      const avgWeight = stats.quantity > 0 ? stats.totalWeight / stats.quantity : 0;

      return {
        id: typeId,
        name: type?.name || 'Unknown',
        biomass: Math.round(stats.biomass),
        avgWeight: avgWeight,
        color: COLORS[index % COLORS.length]
      };
    })
    .filter(item => item.biomass > 0)
    .sort((a, b) => b.biomass - a.biomass);
});

const totalBiomass = computed(() => {
  return summaryData.value.reduce((sum, item) => sum + item.biomass, 0);
});

const chartData = computed(() => {
  return summaryData.value.map(item => ({
    name: item.name,
    value: item.biomass,
    itemStyle: { color: item.color }
  }));
});

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} kg ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    show: false
  },
  series: [
    {
      name: 'Biomass',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '20',
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: chartData.value
    }
  ]
}));
</script>

<style scoped>
.chart-container {
  height: 250px;
  width: 100%;
}
.chart {
  height: 100%;
  width: 100%;
}
</style>

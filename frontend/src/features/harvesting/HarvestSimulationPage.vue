<template>
  <v-container fluid>
    <div class="d-flex align-center mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" @click="router.back()" class="mr-2"></v-btn>
      <div class="d-flex flex-column">
        <h1 class="text-h4 font-weight-bold">Harvest Simulation</h1>
        <span class="text-subtitle-1 text-grey">Project revenue for all active batches by species</span>
      </div>
      <v-spacer></v-spacer>
      <v-btn color="primary" variant="flat" prepend-icon="mdi-refresh" @click="resetSimulation">
        Reset Simulation
      </v-btn>
    </div>

    <!-- Summary Cards -->
    <v-row class="mb-6">
      <v-col cols="12" sm="4">
        <v-card variant="outlined" class="rounded-lg shadow-sm">
          <v-card-text>
            <div class="text-overline mb-1">Total Projected Biomass</div>
            <div class="text-h4 font-weight-bold">{{ totalProjectedBiomass.toLocaleString() }} <span class="text-subtitle-1">KG</span></div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="4">
        <v-card variant="outlined" class="rounded-lg shadow-sm border-success">
          <v-card-text>
            <div class="text-overline mb-1 text-success">Total Projected Revenue</div>
            <div class="text-h4 font-weight-bold text-success">₹{{ totalProjectedRevenue.toLocaleString('en-IN', { maximumFractionDigits: 0 }) }}</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="4">
        <v-card variant="outlined" class="rounded-lg shadow-sm">
          <v-card-text>
            <div class="text-overline mb-1">Total Pieces (Available)</div>
            <div class="text-h4 font-weight-bold">{{ totalQuantity.toLocaleString() }} <span class="text-subtitle-1">pcs</span></div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Simulation Table -->
    <v-card variant="outlined" class="rounded-lg shadow-sm overflow-hidden">
      <v-table hover density="comfortable">
        <thead class="bg-slate-50">
          <tr>
            <th class="font-weight-bold">Batch / Species</th>
            <th class="font-weight-bold text-center">Available Stock (pcs)</th>
            <th class="font-weight-bold text-center" style="width: 150px">Target ABW (g)</th>
            <th class="font-weight-bold text-center">Proj. Biomass (kg)</th>
            <th class="font-weight-bold text-center" style="width: 150px">Base Rate (₹/kg)</th>
            <th class="font-weight-bold text-center">Final Rate (₹)</th>
            <th class="font-weight-bold text-right">Proj. Revenue</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="batch in simulationData" :key="batch.id">
            <tr class="bg-grey-lighten-4">
              <td colspan="7" class="font-weight-bold py-2">
                <v-icon icon="mdi-link-variant" size="small" class="mr-2"></v-icon>
                {{ batch.name }}
              </td>
            </tr>
            <tr v-for="species in batch.species" :key="species.id">
              <td class="pl-8">
                <div class="font-weight-medium text-primary">{{ species.name }}</div>
                <div class="text-caption text-grey">Latest Sample: {{ species.currentABW.toFixed(1) }}g</div>
              </td>
              <td class="text-center">{{ species.quantity.toLocaleString() }}</td>
              <td class="text-center">
                <v-text-field
                  :model-value="speciesConfigs[species.configKey]?.targetABW"
                  @update:model-value="(v: any) => updateConfig(species.configKey, 'targetABW', v)"
                  type="number"
                  variant="outlined"
                  density="compact"
                  hide-details
                  suffix="g"
                  bg-color="white"
                ></v-text-field>
              </td>
              <td class="text-center font-weight-bold">{{ species.biomass.toLocaleString() }}</td>
              <td class="text-center">
                <v-text-field
                  :model-value="speciesConfigs[species.configKey]?.baseRate"
                  @update:model-value="(v: any) => updateConfig(species.configKey, 'baseRate', v)"
                  type="number"
                  variant="outlined"
                  density="compact"
                  hide-details
                  prefix="₹"
                  bg-color="white"
                ></v-text-field>
              </td>
              <td class="text-center">
                <span :class="species.finalRate < species.baseRate ? 'text-orange-darken-3 font-weight-bold' : ''">
                   ₹{{ species.finalRate.toFixed(2) }}
                </span>
                <v-tooltip v-if="species.finalRate < species.baseRate" activator="parent" location="top">
                  Sliding scale applied: ₹108 - (1000 - {{ species.targetABW }}) * 0.02
                </v-tooltip>
              </td>
              <td class="text-right font-weight-bold text-success">
                ₹{{ species.revenue.toLocaleString('en-IN', { maximumFractionDigits: 0 }) }}
              </td>
            </tr>
            <!-- Batch Total Row -->
             <tr class="bg-indigo-lighten-5">
              <td colspan="3" class="text-right font-weight-bold text-indigo-darken-4">Batch Total:</td>
              <td class="text-center font-weight-bold text-indigo-darken-4">{{ getBatchBiomass(batch.id || '').toLocaleString() }} kg</td>
              <td colspan="2"></td>
              <td class="text-right font-weight-bold text-indigo-darken-4">₹{{ getBatchRevenue(batch.id || '').toLocaleString('en-IN', { maximumFractionDigits: 0 }) }}</td>
            </tr>
          </template>
        </tbody>
      </v-table>
    </v-card>

    <!-- Logic Explanation -->
    <v-alert
      variant="tonal"
      class="mt-8 rounded-lg"
      color="info"
      icon="mdi-information"
    >
      <div class="text-subtitle-2 font-weight-bold mb-1">Simulation Logic & Assumptions:</div>
      <ul class="text-caption">
        <li><strong>Projected Biomass</strong> = (Quantity × Target ABW) / 1000.</li>
        <li><strong>Sliding Scale Rate</strong> = Applied if Target ABW is below 1000g: <code>Base Rate - (1000 - Target ABW) × 0.02</code>.</li>
        <li><strong>Net Revenue</strong> = <code>Projected Biomass × Discount Multiplier × Final Rate</code>. Discount: 5% for Fish, 0% for Shrimp.</li>
        <li><strong>Available Quantity</strong> is calculated based on stocking minus actual harvests, multiplied by the latest survival estimate if available.</li>
      </ul>
    </v-alert>

  </v-container>
</template>

<script setup lang="ts">
import {computed, reactive} from 'vue';
import {useRouter} from 'vue-router';
import {useBatches} from '../batches/composables/useBatches';
import {useHarvestLogs} from './composables/useHarvestLogs';
import {sampleLogService} from '../operations/api/sampleLogService';
import {fishTypeService} from '../settings/api/fishTypeService';
import type {Batch} from '../../api/generated/model';

const router = useRouter();
const { batches, getBatchById } = useBatches();
const { harvestLogs } = useHarvestLogs();
const { data: sampleLogs } = sampleLogService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();

// Helpers to safely extract arrays from potentially paginated responses
function getSampleLogsArray(): any[] {
  const data = sampleLogs.value;
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (typeof data === 'object' && Array.isArray((data as any).content)) return (data as any).content;
  return [];
}

function getHarvestLogsArray(): any[] {
  const data = harvestLogs.value;
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (typeof data === 'object' && Array.isArray((data as any).content)) return (data as any).content;
  return [];
}

// Reactive state for simulation parameters
const speciesConfigs = reactive<Record<string, { targetABW: number, baseRate: number }>>({});

const simulationData = computed(() => {
  if (!batches.value) return [];

  return batches.value
    .filter((b: Batch) => b.status === 'Active')
    .map((batch: Batch) => {
      const species = (batch.fishSeeds || []).map((seed: any) => {
        const typeId = seed.fishTypeId || 'unknown';
        const type = fishTypes.value?.find(t => t.id === typeId);
        const name = type?.name || typeId;

        // Calculate current quantity
        const totalStocked = seed.quantity || 0;
        const totalHarvested = getHarvestLogsArray()
          .filter(h => h.batchId === batch.id && h.fishTypeId === typeId)
          .reduce((sum, h) => sum + (h.count || 0), 0);

        let currentQuantity = Math.max(0, totalStocked - totalHarvested);

        // ABW from latest sample
        const latestSample = getSampleLogsArray()
          .filter((s: any) => s.batchId === (batch.id || '') && s.fishTypeId === typeId)
          .sort((a: any, b: any) => new Date(b.date || '').getTime() - new Date(a.date || '').getTime())[0];

        if (latestSample?.survivalEstimate !== undefined) {
          currentQuantity = currentQuantity * (latestSample.survivalEstimate / 100);
        }

        const currentABW = latestSample?.averageWeight || seed.sizeGrams || 0;

        // Use reactive config if exists, otherwise default
        const configKey = `${batch.id}-${typeId}`;
        if (!speciesConfigs[configKey]) {
           speciesConfigs[configKey] = {
             targetABW: currentABW > 0 ? Math.max(currentABW, 1000) : 1000,
             baseRate: 108
           };
        }

        const config = speciesConfigs[configKey];
        const biomass = (currentQuantity * config.targetABW) / 1000;

        let finalRate = config.baseRate;
        if (config.targetABW > 0 && config.targetABW < 1000) {
          finalRate = config.baseRate - (1000 - config.targetABW) * 0.02;
        }

        // Apply discount based on cultivation type: 0% for Shrimp, 5% for Fish
        const discountMultiplier = type?.cultivationType === 'SHRIMP' ? 1.0 : 0.95;
        const revenue = (biomass * discountMultiplier) * finalRate;

        return {
          id: typeId,
          name,
          quantity: currentQuantity,
          currentABW,
          targetABW: config.targetABW,
          baseRate: config.baseRate,
          finalRate,
          biomass,
          revenue,
          configKey
        };
      });

      return {
        id: batch.id,
        name: batch.name || batch.stockingDate,
        species
      };
    });
});

// Helper functions for batch summaries
function getBatchBiomass(batchId: string) {
  const b = simulationData.value.find(b => b.id === batchId);
  return b?.species.reduce((sum, s) => sum + s.biomass, 0) || 0;
}

function getBatchRevenue(batchId: string) {
  const b = simulationData.value.find(b => b.id === batchId);
  return b?.species.reduce((sum, s) => sum + s.revenue, 0) || 0;
}

// Global summaries
const totalProjectedBiomass = computed(() => {
  return simulationData.value.reduce((sum, b) => sum + getBatchBiomass(b.id || ''), 0);
});

const totalProjectedRevenue = computed(() => {
  return simulationData.value.reduce((sum, b) => sum + getBatchRevenue(b.id || ''), 0);
});

const totalQuantity = computed(() => {
  return simulationData.value.reduce((sum, b) =>
    sum + b.species.reduce((sSum, s) => sSum + s.quantity, 0), 0);
});

function resetSimulation() {
  Object.keys(speciesConfigs).forEach(key => delete speciesConfigs[key]);
}

// Function to update config values from input fields
function updateConfig(key: string, field: 'targetABW' | 'baseRate', value: string | number) {
  if (!speciesConfigs[key]) return;
  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  if (!isNaN(numValue)) {
    speciesConfigs[key][field] = numValue;
  }
}


</script>

<style scoped>
.bg-slate-50 {
  background-color: #f8fafc;
}
.shadow-sm {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}
.border-success {
  border-color: #4CAF50 !important;
}
</style>

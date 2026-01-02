<template>
  <v-container fluid>
    <PageHeader title="Dashboard" />

    <!-- Summary Cards -->
    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card color="primary" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Active Ponds</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ activePondsCount }}</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="success" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Biomass Est.</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ totalBiomassEst }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="warning" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Feed Today</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ feedToday }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="info" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Average FCR</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ avgFCR }}</div>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <!-- Running Batches Section -->
    <v-row class="mt-6">
      <v-col cols="12">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4 d-flex align-center flex-wrap">
            <v-icon icon="mdi-calendar-clock" class="mr-2" color="primary"></v-icon>
            <span class="mr-4">Running Batches Timeline</span>
            <v-spacer></v-spacer>
            <div style="width: 250px;">
                <v-text-field
                  v-model="referenceDate"
                  label="View age at date"
                  type="date"
                  variant="outlined"
                  density="compact"
                  hide-details
                  prepend-inner-icon="mdi-calendar-search"
                ></v-text-field>
            </div>
          </v-card-title>
          <v-divider></v-divider>
          <v-table hover>
            <thead>
              <tr class="bg-grey-lighten-4">
                <th class="text-subtitle-2 font-weight-bold">Batch</th>
                <th class="text-subtitle-2 font-weight-bold">Pond</th>
                <th class="text-subtitle-2 font-weight-bold">Start Date</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Days of Culture</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Stocked</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Harvested</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Loss</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Current FCR</th>
                <th class="text-subtitle-2 font-weight-bold">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in activeBatchesWithHistory" :key="batch.id">
                <td class="font-weight-medium text-primary">{{ batch.name || 'Unnamed' }}</td>
                <td>{{ getBatchPonds(batch) }}</td>
                <td>{{ formatDate(batch.stockingDate) }}</td>
                <td class="text-center">
                  <v-chip size="small" variant="tonal" :color="getAgeColor(batch.stockingDate)">
                    {{ calculateAge(batch.stockingDate) }} Days
                  </v-chip>
                </td>
                <td class="text-center font-weight-medium text-grey-darken-1">
                  {{ getBatchStockedQty(batch).toLocaleString() }}
                </td>
                <td class="text-center font-weight-medium text-success">
                  {{ getBatchHarvestedQty(batch).toLocaleString() }}
                </td>
                <td class="text-center font-weight-bold" :class="getBatchLoss(batch) > 0 ? 'text-error' : ''">
                  {{ getBatchLoss(batch).toLocaleString() }}
                </td>
                <td class="text-center font-weight-bold text-primary">
                  {{ calculateBatchFCR(batch) }}
                </td>
                <td>
                  <v-chip
                    :color="getBatchStatusColor(batch.stockingDate)"
                    size="x-small"
                    variant="flat"
                    class="text-uppercase"
                  >
                    {{ getBatchStatusLabel(batch.stockingDate) }}
                  </v-chip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="activeBatches.length === 0" class="text-center py-8 text-grey">
            <v-icon icon="mdi-history" size="48" class="mb-2 opacity-20"></v-icon>
            <div>No running batches.</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-4">
      <!-- Alerts / Notifications -->
      <v-col cols="12" md="6">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4">Recent Alerts</v-card-title>
          <v-divider></v-divider>
          <v-list density="comfortable">
            <v-list-item prepend-icon="mdi-water-alert" title="Low DO in Pond A1" subtitle="30 mins ago" color="error"></v-list-item>
            <v-list-item prepend-icon="mdi-flask-empty-minus-outline" title="Feed stock running low" subtitle="2 hours ago" color="warning"></v-list-item>
          </v-list>
        </v-card>
      </v-col>

      <!-- Quick Stats / Forecast -->
      <v-col cols="12" md="6">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4">Harvest Forecast</v-card-title>
          <v-divider></v-divider>
          <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                  <span class="text-grey-darken-1">Next Expected Harvest:</span>
                  <span class="font-weight-bold">{{ formatDate('2025-01-23') }}</span>
              </div>
              <v-progress-linear model-value="85" color="success" height="12" rounded></v-progress-linear>
              <div class="text-caption mt-2 text-right">85% through the current cycle</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Pivot Tables -->
    <v-row class="mt-4">
        <v-col cols="12">
            <FishStockingPivot :batches="batches || []" />
        </v-col>
    </v-row>

    <v-row class="mb-8">
        <v-col cols="12">
            <SampleLogPivot />
        </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
import { usePonds } from '../ponds/composables/usePonds';
import { useBatches } from '../batches/composables/useBatches';
import { feedLogService } from './api/feedLogService';
import { sampleLogService } from './api/sampleLogService';
import { harvestLogService } from '../harvesting/api/harvestLogService';
import { feedStockService } from '../inventory/api/feedStockService';
import FishStockingPivot from './components/FishStockingPivot.vue';
import SampleLogPivot from './components/SampleLogPivot.vue';
import type { Batch, FeedLog, Pond, HarvestLog, SampleLog } from '../../api/generated/model';

const store = useAquaStore();

// Use composables
const { ponds } = usePonds();
const { batches } = useBatches();
const { data: feedLogs } = feedLogService.useGetAll();
const { data: lowStockItems } = feedStockService.useGetLowStock();
const { data: harvestLogs } = harvestLogService.useGetAll();
const { data: sampleLogs } = sampleLogService.useGetAll();

const referenceDate = ref(new Date().toISOString().substr(0, 10));

const lowStockCount = computed(() => {
    return lowStockItems.value?.length || 0;
});

const activePondsCount = computed(() => {
  return ponds.value?.filter((p: Pond) => p.status === 'Active').length || 0;
});

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active') || [];
});

// Sort batches to show the ones coming up
const activeBatchesWithHistory = computed(() => {
    return [...activeBatches.value].sort((a, b) =>
      new Date(a.stockingDate || '').getTime() - new Date(b.stockingDate || '').getTime()
    );
});

const feedToday = computed(() => {
  const today = new Date().toISOString().substr(0, 10);
  return feedLogs.value
    ?.filter((l: FeedLog) => l.date === today)
    .reduce((sum: number, l: FeedLog) => sum + (l.amount || 0), 0) || 0;
});

const avgFCR = computed(() => {
    const activeWithFCR = activeBatches.value
        .map(b => parseFloat(calculateBatchFCR(b)))
        .filter(fcr => !isNaN(fcr) && fcr > 0);
    
    if (activeWithFCR.length === 0) return '-';
    const sum = activeWithFCR.reduce((a, b) => a + b, 0);
    return (sum / activeWithFCR.length).toFixed(2);
});

function calculateBatchFCR(batch: Batch): string {
    const totalFeed = feedLogs.value
        ?.filter(l => l.batchId === batch.id)
        .reduce((sum, l) => sum + (l.amount || 0), 0) || 0;
    
    if (totalFeed === 0) return '-';

    // 1. Initial Biomass (at stocking)
    const initialBiomass = batch.fishSeeds?.reduce((sum, s) => {
        return sum + ((s.quantity || 0) * (s.sizeGrams || 0)) / 1000;
    }, 0) || 0;

    // 2. Current Biomass Estimate
    const currentBiomass = calculateBatchBiomass(batch);

    // 3. Weight Gained
    const weightGained = currentBiomass - initialBiomass;

    if (weightGained <= 0) return '-';

    return (totalFeed / weightGained).toFixed(2);
}

function calculateBatchBiomass(batch: Batch): number {
    // 1. Base quantity from stocking
    const totalStocked = batch.fishSeeds?.reduce((sum, s) => sum + (s.quantity || 0), 0) || 0;
    if (totalStocked === 0) return 0;

    // 2. Subtract harvests
    const totalHarvested = harvestLogs.value
      ?.filter(l => l.batchId === batch.id)
      .reduce((sum, l) => sum + (l.count || 0), 0) || 0;
    
    let currentQuantity = Math.max(0, totalStocked - totalHarvested);

    // 3. Get latest sampling data
    const allSamples = Array.isArray(sampleLogs.value) 
      ? sampleLogs.value 
      : ((sampleLogs.value as any)?.content || []);
    
    const batchSamples = [...allSamples]
      .filter(l => l.batchId === batch.id)
      .sort((a, b) => new Date(b.date || '').getTime() - new Date(a.date || '').getTime());
    
    const latestSample = batchSamples[0];
    
    // Apply survival estimate if available
    if (latestSample?.survivalEstimate !== undefined) {
        currentQuantity = currentQuantity * (latestSample.survivalEstimate / 100);
    }

    // 4. Determine current average weight (ABW)
    let currentABW = latestSample?.averageWeight;
    
    // Fallback: If no sample, use stocking ABW
    if (!currentABW && batch.fishSeeds && batch.fishSeeds.length > 0) {
        const totalStockWeight = batch.fishSeeds.reduce((sum, s) => sum + ((s.quantity || 0) * (s.sizeGrams || 0)), 0);
        currentABW = totalStockWeight / totalStocked;
    }

    if (currentABW) {
        return (currentQuantity * currentABW) / 1000;
    }
    return 0;
}

function getBatchStockedQty(batch: Batch): number {
    return batch.fishSeeds?.reduce((sum, s) => sum + (s.quantity || 0), 0) || 0;
}

function getBatchHarvestedQty(batch: Batch): number {
    return harvestLogs.value
      ?.filter(l => l.batchId === batch.id)
      .reduce((sum, l) => sum + (l.count || 0), 0) || 0;
}

function getBatchLoss(batch: Batch): number {
    const stocked = getBatchStockedQty(batch);
    const harvested = getBatchHarvestedQty(batch);
    // Loss is difference but only if harvested has started or it's a closed batch.
    // For simplicity as requested, show the difference.
    return Math.max(0, stocked - harvested);
}

const totalBiomassEst = computed(() => {
  if (!batches.value) return 0;
  return Math.round(activeBatches.value.reduce((sum, b) => sum + calculateBatchBiomass(b), 0));
});

function getBatchPonds(batch: Batch) {
  if (!batch.fishSeeds || batch.fishSeeds.length === 0) return 'N/A';
  const pondNames = batch.fishSeeds
    .map(s => ponds.value?.find(p => p.id === s.pondId)?.name)
    .filter(Boolean);
  return [...new Set(pondNames)].join(', ') || 'Unknown';
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-';
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateStr).toLocaleDateString(undefined, options);
}

function calculateAge(stockingDate?: string) {
  if (!stockingDate) return 0;
  const start = new Date(stockingDate).setHours(0,0,0,0);
  const refDate = new Date(referenceDate.value).setHours(0,0,0,0);

  if (refDate < start) return 0; // Hasn't started yet relative to selection

  const diff = refDate - start;
	return Math.floor(diff / (1000 * 60 * 60 * 24));
}

function getAgeColor(stockingDate?: string) {
    const age = calculateAge(stockingDate);
    if (age === 0) return 'grey';
    if (age < 30) return 'cyan';
    if (age < 90) return 'blue';
    return 'indigo';
}

function getBatchStatusLabel(stockingDate?: string) {
    if (!stockingDate) return 'Unknown';
    const start = new Date(stockingDate).getTime();
    const refDate = new Date(referenceDate.value).getTime();
    if (start > refDate) return 'Scheduled';
    return 'Running';
}

function getBatchStatusColor(stockingDate?: string) {
    if (!stockingDate) return 'grey';
    const start = new Date(stockingDate).getTime();
    const refDate = new Date(referenceDate.value).getTime();
    if (start > refDate) return 'info';
    return 'success';
}
</script>

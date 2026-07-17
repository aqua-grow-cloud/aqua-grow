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
             <div class="text-overline mb-1">Avg FCR (Active)</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ avgFCR }}</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="secondary" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Avg FCR (Harvested)</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ avgHarvestedFCR }}</div>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <!-- Row 1: Pond Utilisation + Feed Stock Alerts -->
    <v-row class="mt-6">
      <v-col cols="12" md="8">
        <PondUtilisationWidget />
      </v-col>
      <v-col cols="12" md="4">
        <FeedStockAlertsWidget />
      </v-col>
    </v-row>

    <!-- Row 2: Survival Rate + Water Quality -->
    <v-row class="mt-4">
      <v-col cols="12" md="4">
        <SurvivalRateWidget :sample-logs="allSamples" />
      </v-col>
      <v-col cols="12" md="8">
        <WaterQualityWidget />
      </v-col>
    </v-row>

    <!-- Row 3: ABW Growth Curves (full width) -->
    <v-row class="mt-4">
      <v-col cols="12">
        <ABWGrowthWidget :sample-logs="allSamples" />
      </v-col>
    </v-row>

    <!-- Row 4: Monthly Finance + Batch Profitability -->
    <v-row class="mt-4">
      <v-col cols="12" md="7">
        <MonthlyFinanceWidget :expenditures="expenditures" :harvest-logs="harvestLogs || []" />
      </v-col>
      <v-col cols="12" md="5">
        <BatchProfitabilityWidget
          :expenditures="expenditures"
          :harvest-logs="harvestLogs || []"
          :is-loading="expendituresLoading"
        />
      </v-col>
    </v-row>

    <!-- Running Batches Section -->
    <v-row class="mt-6">
      <v-col cols="12">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4 d-flex align-center flex-wrap">
            <v-icon icon="mdi-calendar-clock" class="mr-2" color="primary"></v-icon>
            <span class="mr-4">Batches Timeline</span>
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
                <th class="text-subtitle-2 font-weight-bold text-center">Balance / Loss</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Current FCR</th>
                <th class="text-subtitle-2 font-weight-bold">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in sortedDashboardBatches" :key="batch.id">
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
                <td class="text-center font-weight-bold">
                  <v-tooltip
                    :text="batch.status === 'Completed' ? 'Final Loss' : 'Remaining Balance'"
                    location="top"
                  >
                    <template v-slot:activator="{ props }">
                      <span
                        v-bind="props"
                        :class="batch.status === 'Completed' && getBatchDiff(batch) > 0 ? 'text-error' : ''"
                      >
                        {{ getBatchDiff(batch).toLocaleString() }}
                      </span>
                    </template>
                  </v-tooltip>
                </td>
                <td class="text-center font-weight-bold text-primary">
                  {{ calculateBatchFCR(batch) }}
                </td>
                <td>
                  <v-chip
                    :color="getBatchStatusColor(batch.status)"
                    size="x-small"
                    variant="flat"
                    class="text-uppercase"
                  >
                    {{ batch.status }}
                  </v-chip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="dashboardBatches.length === 0" class="text-center py-8 text-grey">
            <v-icon icon="mdi-history" size="48" class="mb-2 opacity-20"></v-icon>
            <div>No running batches.</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Pivot Tables -->
    <v-row class="mt-4">
        <v-col cols="12" md="6">
            <FishStockingPivot :batches="batches || []" />
        </v-col>
        <v-col cols="12" md="6">
            <BiomassByFishType
              :batches="dashboardBatches"
              :fish-types="fishTypes || []"
              :harvest-logs="harvestLogs || []"
              :sample-logs="allSamples"
            />
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
import {useQueries} from '@tanstack/vue-query';
import {useAquaStore} from '@/stores/useAquaStore.ts';
import PageHeader from '../../components/PageHeader.vue';
import {usePonds} from '../ponds/composables/usePonds';
import {useBatches} from '../batches/composables/useBatches';
import {feedLogService} from './api/feedLogService';
import {harvestLogService} from '../harvesting/api/harvestLogService';
import {feedStockService} from '../inventory/api/feedStockService';
import {fishTypeService} from '../settings/api/fishTypeService';
import {expenditureService} from '../finance/api/expenditureService';
import {getGetSampleLogsByBatchQueryOptions} from '@/api/generated/sample-log-controller/sample-log-controller.ts';
import FishStockingPivot from './components/FishStockingPivot.vue';
import SampleLogPivot from './components/SampleLogPivot.vue';
import BiomassByFishType from './components/BiomassByFishType.vue';
import WaterQualityWidget from './components/WaterQualityWidget.vue';
import FeedStockAlertsWidget from './components/FeedStockAlertsWidget.vue';
import SurvivalRateWidget from './components/SurvivalRateWidget.vue';
import ABWGrowthWidget from './components/ABWGrowthWidget.vue';
import BatchProfitabilityWidget from './components/BatchProfitabilityWidget.vue';
import MonthlyFinanceWidget from './components/MonthlyFinanceWidget.vue';
import PondUtilisationWidget from './components/PondUtilisationWidget.vue';
import type {Batch, Expenditure, FeedLog, Pond, SampleLog} from '@/api/generated/model';

const store = useAquaStore();

// Use composables
const { ponds } = usePonds();
const { batches } = useBatches();
const { data: feedLogs } = feedLogService.useGetAll();
const { data: lowStockItems } = feedStockService.useGetLowStock();
const { data: harvestLogs } = harvestLogService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();

// Expenditures (all) for financial widgets
const { data: expendituresData, isLoading: expendituresLoading } = expenditureService.useGetAll();
const expenditures = computed<Expenditure[]>(() => (expendituresData.value as Expenditure[]) || []);

const referenceDate = ref(new Date().toISOString().substr(0, 10));

const lowStockCount = computed(() => {
    return lowStockItems.value?.length || 0;
});

const activePondsCount = computed(() => {
  return ponds.value?.filter((p: Pond) => p.status === 'Active').length || 0;
});

const dashboardBatches = computed(() => {
  return batches.value?.filter((b: Batch) =>
    b.status === 'Active' || b.status === 'Completed') || [];
});

// Fetch sample logs for all dashboard batches using unpaginated endpoint
const batchSampleQueries = computed(() => {
    return dashboardBatches.value.map(batch =>
        getGetSampleLogsByBatchQueryOptions(batch.id || '')
    );
});

const sampleQueriesResults = useQueries({
    queries: batchSampleQueries,
    combine: (results) => {
        const allSamples: SampleLog[] = [];
        results.forEach(result => {
            if (result.data) {
                allSamples.push(...result.data);
            }
        });
        return allSamples;
    }
});

const allSamples = computed(() => {
    return sampleQueriesResults.value || [];
});

const sortedDashboardBatches = computed(() => {
    return [...dashboardBatches.value].sort((a, b) => {
      // Show active first
      if (a.status !== b.status) return a.status === 'Active' ? -1 : 1;
      // Then sort by stocking date (newest first)
      return new Date(b.stockingDate || '').getTime() - new Date(a.stockingDate || '').getTime();
    });
});

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active') || [];
});

const feedToday = computed(() => {
  const today = new Date().toISOString().substr(0, 10);
  return feedLogs.value
    ?.filter((l: FeedLog) => l.date === today && dashboardBatches.value.some(b => b.id === l.batchId && b.status === 'Active'))
    .reduce((sum: number, l: FeedLog) => sum + (l.amount || 0), 0) || 0;
});

const avgFCR = computed(() => {
    const batchesWithFCR = dashboardBatches.value
        .filter(b => b.status === 'Active')
        .map(b => parseFloat(calculateBatchFCR(b)))
        .filter(fcr => !isNaN(fcr) && fcr > 0);

    if (batchesWithFCR.length === 0) return '-';
    const sum = batchesWithFCR.reduce((a, b) => a + b, 0);
  return (sum / batchesWithFCR.length).toFixed(2);
});

const avgHarvestedFCR = computed(() => {
    const batchesWithFCR = dashboardBatches.value
        .filter((b: Batch) => b.status === 'Completed')
        .map(b => parseFloat(calculateBatchFCR(b)))
        .filter(fcr => !isNaN(fcr) && fcr > 0);

    if (batchesWithFCR.length === 0) return '-';
    const sum = batchesWithFCR.reduce((a, b) => a + b, 0);
  return (sum / batchesWithFCR.length).toFixed(2);
});

function calculateBatchFCR(batch: Batch): string {
    const totalFeed = feedLogs.value
        ?.filter(l => l.batchId === batch.id)
        .reduce((sum, l) => sum + (l.amount || 0), 0) || 0;

    if (totalFeed === 0) {
        return '-';
    }

    const initialBiomass = batch.fishSeeds?.reduce((sum, s) => {
        let weightGrams = s.sizeGrams || 0;
        if (!weightGrams && s.sizeInches) {
            weightGrams = s.sizeInches * 18;
        }
        return sum + ((s.quantity || 0) * weightGrams) / 1000;
    }, 0) || 0;

    const currentBiomass = calculateBatchBiomass(batch);

    let totalBiomassProduced = currentBiomass;
    if (batch.status === 'Active') {
        const harvestedWeight = harvestLogs.value
            ?.filter(l => l.batchId === batch.id)
            .reduce((sum, l) => sum + (l.actualWeight || l.biomassWeight || 0), 0) || 0;
        totalBiomassProduced = currentBiomass + harvestedWeight;
    }

    const weightGained = totalBiomassProduced - initialBiomass;

    if (weightGained <= 0) return '-';

    return (totalFeed / weightGained).toFixed(2);
}

function calculateBatchBiomass(batch: Batch): number {
    if (batch.status === 'Completed') {
        return harvestLogs.value
          ?.filter(l => l.batchId === batch.id)
          .reduce((sum, l) => sum + (l.actualWeight || l.biomassWeight || 0), 0) || 0;
    }

    const totalStocked = batch.fishSeeds?.reduce((sum, s) => sum + (s.quantity || 0), 0) || 0;
    if (totalStocked === 0) return 0;

    const totalHarvested = harvestLogs.value
      ?.filter(l => l.batchId === batch.id)
      .reduce((sum, l) => sum + (l.count || 0), 0) || 0;

    let currentQuantity = Math.max(0, totalStocked - totalHarvested);

    const batchSamples = allSamples.value.filter(l => l.batchId === batch.id);

    if (batchSamples.length === 0) {
        let currentABW = 0;
        if (batch.fishSeeds && batch.fishSeeds.length > 0) {
            const totalStockWeight = batch.fishSeeds.reduce((sum, s) => {
                let weightGrams = s.sizeGrams || 0;
                if (!weightGrams && s.sizeInches) {
                    weightGrams = s.sizeInches * 18;
                }
                return sum + ((s.quantity || 0) * weightGrams);
            }, 0);
            currentABW = totalStockWeight / totalStocked;
        }
        return currentABW ? (currentQuantity * currentABW) / 1000 : 0;
    }

    const samplesByDate = batchSamples.reduce((acc, sample) => {
        const date = sample.date || '';
        if (!acc[date]) acc[date] = [];
        acc[date].push(sample);
        return acc;
    }, {} as Record<string, SampleLog[]>);

    const dates = Object.keys(samplesByDate);
    if (dates.length === 0) return 0;

    const latestDate = dates.sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0];
    if (!latestDate) return 0;

    const latestDaySamples = samplesByDate[latestDate];
    if (!latestDaySamples || latestDaySamples.length === 0) return 0;

    let currentABW = 0;
    let latestSurvivalEstimate: number | undefined;

    const weights = latestDaySamples
      .map((s: SampleLog) => s.averageWeight)
      .filter((w): w is number => w != null);

    if (weights.length > 0) {
        currentABW = weights.reduce((acc, w) => acc + w, 0) / weights.length;
    }

    const survivalEstimates = latestDaySamples
      .map((s: SampleLog) => s.survivalEstimate)
      .filter((v): v is number => v != null);

    if (survivalEstimates.length > 0) {
        latestSurvivalEstimate = survivalEstimates[survivalEstimates.length - 1];
    }

    if (latestSurvivalEstimate !== undefined) {
        currentQuantity = currentQuantity * (latestSurvivalEstimate / 100);
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

function getBatchDiff(batch: Batch): number {
    const stocked = getBatchStockedQty(batch);
    const harvested = getBatchHarvestedQty(batch);
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

  if (refDate < start) return 0;

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

function getBatchStatusColor(status?: string) {
    if (status === 'Active') return 'success';
    if (status === 'Completed') return 'info';
    return 'grey';
}
</script>

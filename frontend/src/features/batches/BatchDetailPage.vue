<template>
  <v-container fluid>
    <div class="d-flex align-center mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" @click="router.back()" class="mr-2"></v-btn>
      <div>
        <h1 class="text-h4 font-weight-bold">{{ batch?.name || 'Batch Details' }}</h1>
        <div class="text-subtitle-1 text-grey" v-if="batch">
          Stocked on {{ formatDate(batch.stockingDate) }} • {{ batch.status }}
        </div>
      </div>
      <v-spacer></v-spacer>
    </div>

    <v-row v-if="isLoading">
      <v-col cols="12" class="text-center py-12">
        <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      </v-col>
    </v-row>

    <template v-else-if="batch">
      <v-row>
        <!-- Chart Section -->
        <v-col cols="12" lg="8">
          <FishStockChart
            :events="events || []"
            :fish-types="fishTypes || []"
            :loading="eventsLoading"
          />
        </v-col>

        <!-- Summary Stats Section -->
        <v-col cols="12" lg="4">
          <v-row dense>
             <v-col cols="12">
                <v-card class="rounded-lg mb-4" elevation="2">
                  <v-card-text>
                    <div class="text-overline mb-1">Current Stock</div>
                    <div class="text-h4 font-weight-bold text-primary">{{ currentStock.toLocaleString() }} pcs</div>
                    <v-divider class="my-3"></v-divider>
                    <div class="d-flex justify-space-between text-body-2 mb-1">
                      <span class="text-grey">Total Stocked:</span>
                      <span class="font-weight-bold">{{ totalStocked.toLocaleString() }} pcs</span>
                    </div>
                    <div class="d-flex justify-space-between text-body-2">
                      <span class="text-grey">Total Harvested:</span>
                      <span class="font-weight-bold text-success">{{ totalHarvested.toLocaleString() }} pcs</span>
                    </div>
                  </v-card-text>
                </v-card>
             </v-col>

             <v-col cols="12">
                <v-card class="rounded-lg" elevation="2">
                  <v-card-text>
                    <div class="text-overline mb-1">Estimated Biomass</div>
                    <div class="text-h4 font-weight-bold text-secondary">{{ currentBiomass.toFixed(1) }} kg</div>
                    <v-divider class="my-3"></v-divider>
                    <div class="d-flex justify-space-between text-body-2 mb-1">
                      <span class="text-grey">Avg Weight (ABW):</span>
                      <span class="font-weight-bold">{{ latestABW.toFixed(1) }} g</span>
                    </div>
                    <div class="d-flex justify-space-between text-body-2">
                      <span class="text-grey">Survival (Est.):</span>
                      <span class="font-weight-bold">{{ survivalRate }}%</span>
                    </div>
                  </v-card-text>
                </v-card>
             </v-col>
          </v-row>
        </v-col>
      </v-row>

      <!-- Fish Seeds Table -->
      <v-row class="mt-4">
        <v-col cols="12">
          <v-card class="rounded-lg" elevation="2">
            <v-card-title class="py-4 px-6 d-flex align-center">
              <v-icon color="primary" class="mr-2">mdi-fish</v-icon>
              Fish Seeds
            </v-card-title>
            <v-divider></v-divider>
            <v-data-table
              :headers="seedHeaders"
              :items="batch.fishSeeds || []"
              class="elevation-0"
              density="compact"
            >
              <template v-slot:item.fishTypeId="{ item }">
                {{ getFishTypeName((item as any).fishTypeId) }}
              </template>
              <template v-slot:item.date="{ item }">
                {{ formatDate(item.date) }}
              </template>
              <template v-slot:item.status="{ item }">
                <v-chip size="x-small" :color="item.status === 'Active' ? 'success' : 'grey'" variant="tonal">
                  {{ item.status }}
                </v-chip>
              </template>
              <template v-slot:item.mortality="{ item }">
                <span class="text-error font-weight-bold">
                  {{ getMortalityForSeed((item as any).id).toLocaleString() }}
                </span>
              </template>
              <template v-slot:item.actions="{ item }">
                <v-btn
                  size="x-small"
                  color="error"
                  variant="tonal"
                  prepend-icon="mdi-skull-outline"
                  @click="openMortalityDialog(item as any)"
                >
                  Log Mortality
                </v-btn>
              </template>
            </v-data-table>
          </v-card>
        </v-col>
      </v-row>

      <!-- Event Ledger Table -->
      <v-row class="mt-4">
        <v-col cols="12">
          <v-card class="rounded-lg" elevation="2">
            <v-card-title class="py-4 px-6 d-flex align-center">
              <v-icon color="primary" class="mr-2">mdi-format-list-bulleted</v-icon>
              Event Ledger
            </v-card-title>
            <v-divider></v-divider>
            <v-data-table
              :headers="headers"
              :items="events || []"
              :loading="eventsLoading"
              class="elevation-0"
              density="compact"
            >
              <template v-slot:item.date="{ item }">
                {{ formatDate(item.date) }}
              </template>
              <template v-slot:item.type="{ item }">
                <v-chip :color="getTypeColor(item.type)" size="x-small" class="text-uppercase font-weight-bold" variant="tonal">
                  {{ item.type }}
                </v-chip>
              </template>
              <template v-slot:item.quantityDelta="{ item }">
                <span :class="item.quantityDelta > 0 ? 'text-success font-weight-bold' : item.quantityDelta < 0 ? 'text-error font-weight-bold' : ''">
                  {{ item.quantityDelta > 0 ? '+' : '' }}{{ item.quantityDelta.toLocaleString() }}
                </span>
              </template>
              <template v-slot:item.fishTypeId="{ item }">
                {{ getFishTypeName((item as any).fishTypeId) }}
              </template>
              <template v-slot:item.avgWeightGram="{ item }">
                {{ item.avgWeightGram ? item.avgWeightGram.toFixed(1) + ' g' : '-' }}
              </template>
            </v-data-table>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <v-alert v-else type="error" variant="tonal" class="mt-4">
      Batch not found or failed to load.
    </v-alert>
  </v-container>

  <!-- Mortality Dialog -->
  <SeedMortalityDialog
    v-model="mortalityDialog"
    :batch-id="batchId"
    :seed="selectedSeed"
    :fish-types="(fishTypes as any) || []"
    @saved="handleMortalitySaved"
  />
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useBatches} from '../batches/composables/useBatches';
import {useGetFishStockEventsByBatch} from '@/api/manual/hooks';
import FishStockChart from '../batches/components/FishStockChart.vue';
import SeedMortalityDialog from '../batches/components/SeedMortalityDialog.vue';
import {fishTypeService} from '../settings/api/fishTypeService';
import {FishStockEventType} from '@/api/manual/types';
import type {FishSeed} from '@/api/generated/model';
import {useAquaStore} from '@/stores/useAquaStore';

const route = useRoute();
const router = useRouter();
const batchId = route.params.id as string;
const store = useAquaStore();

const { batches, isLoading, logMortality } = useBatches();
const batch = computed(() => batches.value?.find(b => b.id === batchId));

const { data: events, isLoading: eventsLoading } = useGetFishStockEventsByBatch(batchId);
const { data: fishTypes } = fishTypeService.useGetAll();

const headers = [
  { title: 'Date', key: 'date' },
  { title: 'Type', key: 'type' },
  { title: 'Species', key: 'fishTypeId' },
  { title: 'Delta', key: 'quantityDelta', align: 'end' as const },
  { title: 'ABW', key: 'avgWeightGram', align: 'end' as const },
  { title: 'Note', key: 'referenceId' }
];

const seedHeaders = [
  { title: 'Species', key: 'fishTypeId' },
  { title: 'Date', key: 'date' },
  { title: 'Qty Stocked', key: 'quantity', align: 'end' as const },
  { title: 'Dead', key: 'mortality', align: 'end' as const },
  { title: 'Status', key: 'status' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
];

// Mortality dialog state
const mortalityDialog = ref(false);
const selectedSeed = ref<FishSeed | null>(null);

function openMortalityDialog(seed: FishSeed) {
  selectedSeed.value = seed;
  mortalityDialog.value = true;
}

function getMortalityForSeed(seedId: string): number {
  if (!events.value || !seedId) return 0;
  return events.value
    .filter(e => e.type === FishStockEventType.MORTALITY && (e as any).referenceId === seedId)
    .reduce((sum, e) => sum + Math.abs(e.quantityDelta), 0);
}

async function handleMortalitySaved(payload: { batchId: string; seedId: string; quantity: number; date: string; note?: string }) {
  try {
    await logMortality(payload);
    store.notify('Mortality logged successfully', 'success');
  } catch {
    store.notify('Failed to log mortality', 'systemError');
  }
}

function getFishTypeName(id?: string) {
    if (!id) return '-';
    return (fishTypes.value as any)?.find((t: any) => t.id === id)?.name || id;
}

const totalStocked = computed(() => {
  return events.value?.filter(e => e.type === FishStockEventType.SEED || e.type === FishStockEventType.TRANSFER_IN)
    .reduce((sum, e) => sum + e.quantityDelta, 0) || 0;
});

const totalHarvested = computed(() => {
  return events.value?.filter(e => e.type === FishStockEventType.HARVEST)
    .reduce((sum, e) => sum + Math.abs(e.quantityDelta), 0) || 0;
});

const currentStock = computed(() => {
  return events.value?.reduce((sum, e) => sum + e.quantityDelta, 0) || 0;
});

const latestABW = computed(() => {
    if (!events.value || events.value.length === 0) return 0;

    // Filter events with weight data and sort by date (newest first)
    const withWeight = events.value
        .filter(e => e.avgWeightGram && e.avgWeightGram > 0)
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    if (withWeight.length === 0) return 0;

    // Get the most recent date
    const mostRecentDate = withWeight[0].date;

    // Get all samples from the most recent date
    const sameDaySamples = withWeight.filter(e => e.date === mostRecentDate);

    // Calculate average of all same-day samples
    const avgWeight = sameDaySamples.reduce((sum, e) => sum + (e.avgWeightGram || 0), 0) / sameDaySamples.length;

    return avgWeight;
});

const currentBiomass = computed(() => {
    return (currentStock.value * latestABW.value) / 1000;
});

const survivalRate = computed(() => {
    if (totalStocked.value === 0) return 0;
    // This is a rough estimate: (current + harvested) / stocked
    const rate = ((currentStock.value + totalHarvested.value) / totalStocked.value) * 100;
    return Math.min(100, Math.round(rate));
});

function formatDate(dateStr?: string) {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString();
}

function getTypeColor(type: FishStockEventType) {
  switch (type) {
    case FishStockEventType.SEED: return 'primary';
    case FishStockEventType.HARVEST: return 'success';
    case FishStockEventType.MORTALITY: return 'error';
    case FishStockEventType.TRANSFER_IN: return 'info';
    case FishStockEventType.TRANSFER_OUT: return 'warning';
    case FishStockEventType.SAMPLING: return 'secondary';
    default: return 'grey';
  }
}
</script>

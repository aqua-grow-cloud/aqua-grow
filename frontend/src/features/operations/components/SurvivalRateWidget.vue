<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center">
      <v-icon icon="mdi-fish" color="teal" class="mr-2"></v-icon>
      Survival Rate — Active Batches
    </v-card-title>

    <div v-if="activeBatches.length === 0" class="text-center py-10 text-grey">
      <v-icon icon="mdi-fish-off" size="40" class="mb-2 opacity-30"></v-icon>
      <div>No active batches</div>
    </div>

    <v-list v-else density="compact" class="py-2">
      <v-list-item
        v-for="item in batchSurvivalList"
        :key="item.batchId"
        class="py-3"
      >
        <v-list-item-title class="font-weight-medium mb-1 d-flex align-center justify-space-between">
          <span>{{ item.batchName }}</span>
          <v-chip
            :color="survivalColor(item.survival)"
            size="x-small"
            variant="flat"
          >
            {{ item.survival != null ? item.survival.toFixed(1) + '%' : '—' }}
          </v-chip>
        </v-list-item-title>

        <v-list-item-subtitle>
          <v-progress-linear
            :model-value="item.survival ?? 0"
            :color="survivalColor(item.survival)"
            height="10"
            rounded
            bg-color="grey-lighten-3"
          ></v-progress-linear>
          <div class="d-flex justify-space-between mt-1 text-caption text-grey">
            <span>DOC: {{ item.doc }} days</span>
            <span>Latest ABW: {{ item.latestABW != null ? item.latestABW.toFixed(1) + ' g' : '—' }}</span>
            <span>Sampled: {{ item.lastSampled || '—' }}</span>
          </div>
        </v-list-item-subtitle>
      </v-list-item>
    </v-list>

    <!-- Legend -->
    <div class="px-6 pb-4 d-flex gap-4 text-caption text-grey">
      <span><v-icon size="x-small" color="success">mdi-circle</v-icon> ≥80% Good</span>
      <span><v-icon size="x-small" color="warning">mdi-circle</v-icon> 60–80% Watch</span>
      <span><v-icon size="x-small" color="error">mdi-circle</v-icon> &lt;60% Critical</span>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useBatches} from '../../batches/composables/useBatches';
import type {SampleLog} from '@/api/generated/model';

const props = defineProps<{
  sampleLogs: SampleLog[];
}>();

const { batches } = useBatches();

const activeBatches = computed(() =>
  batches.value?.filter(b => b.status === 'Active') || []
);

const batchSurvivalList = computed(() =>
  activeBatches.value.map(batch => {
    const batchSamples = props.sampleLogs
      .filter(s => s.batchId === batch.id)
      .sort((a, b) => (b.date || '').localeCompare(a.date || ''));

    const latest = batchSamples[0] ?? null;
    const doc = batch.stockingDate
      ? Math.floor((Date.now() - new Date(batch.stockingDate).getTime()) / 86_400_000)
      : 0;

    return {
      batchId: batch.id!,
      batchName: batch.name || batch.stockingDate || batch.id!,
      survival: latest?.survivalEstimate ?? null,
      latestABW: latest?.averageWeight ?? null,
      lastSampled: latest?.date ?? null,
      doc,
    };
  }).sort((a, b) => (a.survival ?? 101) - (b.survival ?? 101))
);

function survivalColor(val: number | null): string {
  if (val == null) return 'grey';
  if (val >= 80) return 'success';
  if (val >= 60) return 'warning';
  return 'error';
}
</script>

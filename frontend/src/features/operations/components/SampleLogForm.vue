<template>
  <v-card>
    <v-card-title>Sampling & Water Quality</v-card-title>
    <v-card-text>
      <v-form @submit.prevent="save">
        <v-row>
           <v-col cols="12" sm="4">
            <v-select
              v-model="form.batchId"
              :items="activeBatches"
              item-title="label"
              item-value="id"
              label="Select Batch"
              variant="outlined"
              density="compact"
              required
            ></v-select>
          </v-col>
           <v-col cols="12" sm="4">
            <v-select
              v-model="form.fishType"
              :items="availableFishTypes"
              label="Fish Type"
              variant="outlined"
              density="compact"
              :disabled="!form.batchId"
              required
            ></v-select>
          </v-col>
           <v-col cols="12" sm="4">
            <v-text-field
              v-model="form.date"
              label="Date"
              type="date"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
          </v-col>
        </v-row>

        <v-divider class="mb-4"></v-divider>
        <div class="text-subtitle-2 mb-2">Biometrics</div>

        <v-row>
          <v-col cols="6" sm="3">
            <v-text-field
              v-model.number="form.sampleCount"
              label="Pieces (Count)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
            <v-text-field
              v-model.number="form.totalSampleWeight"
              label="Total Weight (g)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
           <v-col cols="6" sm="3">
            <v-text-field
              v-model.number="calculatedABW"
              label="Avg Weight (g)"
              type="number"
              variant="outlined"
              density="compact"
              readonly
              bg-color="grey-lighten-4"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
            <v-text-field
              v-model.number="form.averageLength"
              label="Avg Length (cm)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-btn color="primary" type="submit" :disabled="!isValid" class="mt-2 text-capitalize">Log Sample</v-btn>
      </v-form>

      <v-divider class="my-4"></v-divider>

      <div class="d-flex justify-space-between align-center mb-2">
         <h3 class="text-subtitle-1 font-weight-bold">Recent Samples</h3>
         <v-btn variant="text" color="primary" size="small" :to="{ name: 'sample-logs-history' }">
            View Full History
            <v-icon end>mdi-arrow-right</v-icon>
         </v-btn>
      </div>
       <v-table density="compact" class="border rounded">
        <thead>
          <tr class="bg-grey-lighten-4">
            <th>Date</th>
            <th>Pond</th>
            <th>Species</th>
            <th>Pieces</th>
            <th>Total Weight</th>
            <th>Weight/Unit</th>
            <th>BATCH</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in recentLogs" :key="log.id">
            <td>{{ log.date }}</td>
            <td>{{ getPondName(log.batchId) }}</td>
            <td class="font-weight-medium text-primary">{{ log.fishType || getSpecies(log.batchId) }}</td>
            <td>{{ log.sampleCount }}</td>
            <td>{{ log.totalSampleWeight }} g</td>
            <td class="font-weight-bold">{{ (log.averageWeight || 0).toFixed(1) }} g</td>
            <td class="text-caption text-grey">{{ getBatchDate(log.batchId) }}</td>
          </tr>
        </tbody>
      </v-table>

    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useQueryClient} from '@tanstack/vue-query';
import {useAquaStore} from '@/stores/useAquaStore.ts';
import {useBatches} from '../../batches/composables/useBatches';
import {usePonds} from '../../ponds/composables/usePonds';
import {sampleLogService} from '../api/sampleLogService';
import type {Batch, SampleLog} from '@/api/manual/types.ts';
import { useApiAction } from '@/composables/useApiAction.ts';

const store = useAquaStore();
const queryClient = useQueryClient();

// Use composables
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();
const { data: sampleLogs } = sampleLogService.useGetAll();
const logSampleMutation = sampleLogService.useLog();

const { execute: executeLogSample, isLoading: isLogging, error: logError } = useApiAction(
    () => logSampleMutation.mutateAsync({ data: form.value } as any),
    { successMessage: 'Sample logged successfully' }
);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pondNames = [...new Set(b.fishSeeds?.map(s => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    const speciesList = [...new Set(b.fishSeeds?.map(s => s.type) || [])].join(', ') || 'Unknown';
    return {
      id: b.id,
      label: `${pondNames} - ${speciesList}`
    };
  }) || [];
});

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  fishType: '',
  sampleCount: 0,
  totalSampleWeight: 0,
  averageWeight: 0,
  averageLength: 0,
  survivalEstimate: 100
};

const form = ref({ ...defaultForm });

// Auto-calculate ABW
const calculatedABW = computed(() => {
    if (form.value.sampleCount > 0 && form.value.totalSampleWeight > 0) {
        return parseFloat((form.value.totalSampleWeight / form.value.sampleCount).toFixed(2));
    }
    return 0;
});

// Update the actual form value when calculation changes
watch(calculatedABW, (newVal) => {
    form.value.averageWeight = newVal;
});

// Watch batch changes to reset fishType
watch(() => form.value.batchId, () => {
    form.value.fishType = '';
});

// Compute available fish types for selected batch
const availableFishTypes = computed(() => {
    if (!form.value.batchId) return [];
    const batch = getBatchById(form.value.batchId);
    if (!batch?.fishSeeds) return [];

    return [...new Set(batch.fishSeeds.map((s: any) => s.type))];
});

const isValid = computed(() => form.value.batchId && form.value.fishType && form.value.sampleCount > 0);

const recentLogs = computed(() => {
    const raw = sampleLogs.value;
    const logs = (raw && 'content' in raw && Array.isArray((raw as any).content))
        ? (raw as any).content
        : (Array.isArray(raw) ? raw : []);

    return [...logs].sort((a: SampleLog, b: SampleLog) =>
      new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
    ).slice(0, 5);
});

function getPondName(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    if (!batch) return '-';
    return [...new Set(batch.fishSeeds?.map((s: any) => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || '-';
}

function getSpecies(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    return [...new Set(batch?.fishSeeds?.map((s: any) => s.type) || [])].join(', ') || '-';
}

function getBatchDate(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    return batch?.stockingDate || '-';
}

async function save() {
  await executeLogSample();
  if (!logError.value) {
      queryClient.invalidateQueries({ queryKey: ['api', 'sample-logs'] });
      form.value = { ...defaultForm };
  }
}
</script>

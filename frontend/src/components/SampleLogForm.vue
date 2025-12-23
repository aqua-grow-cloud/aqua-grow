<template>
  <v-card>
    <v-card-title>Sampling & Water Quality</v-card-title>
    <v-card-text>
      <v-form @submit.prevent="save">
        <v-row>
           <v-col cols="12" sm="6">
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
           <v-col cols="12" sm="6">
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

        <v-divider class="mb-4"></v-divider>
        <div class="text-subtitle-2 mb-2">Water Quality</div>

        <v-row>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.do"
              label="DO (mg/L)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.ph"
              label="pH"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.temperature"
              label="Temp (°C)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
           <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.ammonia"
              label="Ammonia"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.salinity"
              label="Salinity (ppt)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
           <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.survivalEstimate"
              label="Est. Survival (%)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-btn color="primary" type="submit" :disabled="!isValid" class="mt-2 text-capitalize">Log Sample</v-btn>
      </v-form>

      <v-divider class="my-4"></v-divider>
      
      <h3 class="text-subtitle-1 mb-2 font-weight-bold">Recent Samples</h3>
       <v-table density="compact" class="border rounded">
        <thead>
          <tr class="bg-grey-lighten-4">
            <th>Date</th>
            <th>Pond</th>
            <th>Fish</th>
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
            <td>{{ getSpecies(log.batchId) }}</td>
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
import { ref, computed, watch } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import { useBatches } from '../composables/useBatches';
import { usePonds } from '../composables/usePonds';
import { sampleLogService } from '../services/api';
import type { Batch, SampleLog } from '../api/generated/model';

const store = useAquaStore();

// Use composables
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();
const { data: sampleLogs } = sampleLogService.useGetAll();

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pond = getPondById(b.pondId);
    return {
      id: b.id,
      label: `${pond ? pond.name : 'Unknown'} - ${b.species}`
    };
  }) || [];
});

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  sampleCount: 0,
  totalSampleWeight: 0,
  averageWeight: 0,
  averageLength: 0,
  survivalEstimate: 100,
  waterQuality: {
    do: 0,
    ph: 7.5,
    temperature: 28,
    ammonia: 0,
    salinity: 0,
    turbidity: 0
  }
};

const form = ref(JSON.parse(JSON.stringify(defaultForm))); // Deep copy for nested obj

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

const isValid = computed(() => form.value.batchId && form.value.sampleCount > 0);

const recentLogs = computed(() => {
    return [...(sampleLogs.value || [])].sort((a: SampleLog, b: SampleLog) => 
      new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
    ).slice(0, 5);
});

function getPondName(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    if (!batch) return '-';
    const pond = getPondById(batch.pondId);
    return pond ? pond.name : '-';
}

function getSpecies(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    return batch?.species || '-';
}

function getBatchDate(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    return batch?.stockingDate || '-';
}

function save() {
  store.logSample({ ...form.value });
  // Reset fields
  form.value = JSON.parse(JSON.stringify(defaultForm));
}
</script>

<template>
  <v-card>
    <v-card-title>Water Quality Monitoring</v-card-title>
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
        <div class="text-subtitle-2 mb-2">Parameters</div>

        <v-row>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.doLevel"
              label="DO (mg/L)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.ph"
              label="pH"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.temperature"
              label="Temp (°C)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.ammonia"
              label="Ammonia"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.salinity"
              label="Salinity (ppt)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.turbidity"
              label="Turbidity"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-btn color="secondary" type="submit" :disabled="!isValid" class="mt-2 text-capitalize">Log Water Quality</v-btn>
      </v-form>

      <v-divider class="my-4"></v-divider>

      <div class="d-flex justify-space-between align-center mb-2">
         <h3 class="text-subtitle-1 font-weight-bold">Recent Monitoring Logs</h3>
         <v-btn variant="text" color="primary" size="small" :to="{ name: 'monitoring-logs-history' }">
            View Full History
            <v-icon end>mdi-arrow-right</v-icon>
         </v-btn>
      </div>

       <v-table density="compact" class="border rounded">
        <thead>
          <tr class="bg-grey-lighten-4">
            <th>Date</th>
            <th>Batch</th>
            <th>DO</th>
            <th>pH</th>
            <th>Temp</th>
            <th>Ammonia</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in recentLogs" :key="log.id">
            <td>{{ log.date }}</td>
             <td>{{ getBatchName(log.batchId) }}</td>
            <td>{{ log.doLevel || '-' }}</td>
            <td>{{ log.ph || '-' }}</td>
            <td>{{ log.temperature || '-' }} °C</td>
            <td>{{ log.ammonia || '-' }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useQueryClient } from '@tanstack/vue-query';
import { useBatches } from '../../batches/composables/useBatches';
import { usePonds } from '../../ponds/composables/usePonds';
import { monitoringLogService } from '../api/monitoringLogService';
import { fishTypeService } from '../../settings/api/fishTypeService';
import type { Batch, MonitoringLog } from '../../../api/manual/types';
import { useApiAction } from '../../../composables/useApiAction';

const queryClient = useQueryClient();
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();

const { data: monitoringLogs } = monitoringLogService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();
const logMonitoringMutation = monitoringLogService.useLog();

const { execute: executeLogMonitoring, isLoading, error } = useApiAction(
    () => logMonitoringMutation.mutateAsync({ data: form.value } as any),
    { successMessage: 'Monitoring log saved' }
);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pondNames = [...new Set(b.fishSeeds?.map(s => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    const speciesList = [...new Set(b.fishSeeds?.map(s => getFishTypeName((s as any).fishTypeId)) || [])].join(', ') || 'Unknown';
    return {
      id: b.id,
      label: `${pondNames} - ${speciesList}`
    };
  }) || [];
});

function getFishTypeName(id?: string) {
    if (!id) return '-';
    return fishTypes.value?.find(t => t.id === id)?.name || id;
}

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  doLevel: 0,
  ph: 7.5,
  temperature: 28,
  ammonia: 0,
  salinity: 0,
  turbidity: 0
};

const form = ref({ ...defaultForm });

const isValid = computed(() => form.value.batchId && form.value.date);

const recentLogs = computed(() => {
    const raw = monitoringLogs.value;
    const logs = (raw && 'content' in raw && Array.isArray((raw as any).content)) 
        ? (raw as any).content 
        : (Array.isArray(raw) ? raw : []);
        
    return [...logs].sort((a: MonitoringLog, b: MonitoringLog) =>
      new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
    ).slice(0, 5);
});

function getBatchName(batchId?: string) {
    if (!batchId) return '-';
    return getBatchById(batchId)?.name || '-';
}

async function save() {
  await executeLogMonitoring();
  if (!error.value) {
      queryClient.invalidateQueries({ queryKey: ['monitoring-logs'] });
      form.value = { ...defaultForm };
  }
}
</script>

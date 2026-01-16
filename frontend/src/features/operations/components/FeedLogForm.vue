<template>
  <v-card>
    <v-card-title>Daily Feed Log</v-card-title>
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
          <v-col cols="12" sm="4">
            <v-text-field
              v-model="form.time"
              label="Time"
              type="time"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
          </v-col>
           <v-col cols="12" sm="4">
            <v-select
              v-model="form.feedType"
              :items="inventoryItems"
              label="Feed Type"
              variant="outlined"
              density="compact"
              required
            ></v-select>
          </v-col>
          <v-col cols="12" sm="2">
            <v-text-field
              v-model.number="displayAmount"
              :label="`Amount (${unit})`"
              type="number"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="2">
            <v-btn-toggle
              v-model="unit"
              mandatory
              color="primary"
              variant="outlined"
              density="compact"
            >
              <v-btn value="kg">Kg</v-btn>
              <v-btn value="bags">Bags</v-btn>
            </v-btn-toggle>
          </v-col>
        </v-row>
        <v-btn color="primary" type="submit" :disabled="!isValid">Log Feed</v-btn>
      </v-form>

      <v-divider class="my-4"></v-divider>

      <h3 class="text-subtitle-1 mb-2">Recent Logs</h3>
      <v-table density="compact">
        <thead>
          <tr>
            <th>Date</th>
            <th>Batch/Pond</th>
            <th>Feed</th>
            <th>Amount</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in recentLogs" :key="log.id">
            <td>{{ log.date }}</td>
            <td>{{ getBatchLabel(log.batchId) }}</td>
            <td>{{ log.feedType }}</td>
            <td>{{ log.amount }} kg</td>
            <td>{{ log.time }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useQueryClient } from '@tanstack/vue-query';
import { useBatches } from '../../batches/composables/useBatches';
import { usePonds } from '../../ponds/composables/usePonds';
import { feedLogService } from '../api/feedLogService';
import { feedStockService } from '../../inventory/api/feedStockService';
import { fishTypeService } from '../../settings/api/fishTypeService';
import type { Batch, FeedLog } from '../../../api/generated/model';
import { useApiAction } from '../../../composables/useApiAction';

const queryClient = useQueryClient();

// Use composables
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();
const { data: feedLogs } = feedLogService.useGetAll();
const { data: feedStock } = feedStockService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();
const logFeedMutation = feedLogService.useLog();

const { execute: executeLogFeed, isLoading: isLogging, error: logError } = useApiAction(
    () => logFeedMutation.mutateAsync({ data: form.value } as any),
    { successMessage: 'Feed logged successfully' }
);

const inventoryItems = computed(() => {
  return feedStock.value?.map(s => s.feedType).filter(Boolean) || [];
});

const unit = ref<'kg' | 'bags'>('kg');
const displayAmount = ref(0);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pondNames = [...new Set(b.fishSeeds?.map(s => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    const speciesList = [...new Set(b.fishSeeds?.map(s => getFishTypeName((s as any).fishTypeId)) || [])].join(', ') || 'Unknown';
    return {
      id: b.id,
      label: `${b.name} (${pondNames} - ${speciesList})`
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
  time: '09:00',
  feedType: '',
  amount: 0
};

const form = ref({ ...defaultForm });

const isValid = computed(() => form.value.batchId && displayAmount.value > 0 && form.value.feedType);

const recentLogs = computed(() => {
  return [...(feedLogs.value || [])].sort((a: FeedLog, b: FeedLog) => 
    new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
  ).slice(0, 5);
});

function getBatchLabel(batchId?: string) {
    if (!batchId) return 'Unknown';
    const batch = getBatchById(batchId);
    if (!batch) return batchId;
    const pondNames = [...new Set(batch.fishSeeds?.map((s: any) => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    return `${batch.name} (${pondNames})`;
}

async function save() {
  // Convert bags to kg if needed (1 bag = 40kg)
  form.value.amount = unit.value === 'bags' ? displayAmount.value * 40 : displayAmount.value;
  
  await executeLogFeed();
  
  if (!logError.value) {
    // Invalidate queries regardless of success/fail to be safe, but usually on success
    queryClient.invalidateQueries({ queryKey: ['api', 'feed-logs'] });
    queryClient.invalidateQueries({ queryKey: ['api', 'feed-stock'] });
    
    // Reset amount
    displayAmount.value = 0;
    form.value.amount = 0;
  }
}
</script>

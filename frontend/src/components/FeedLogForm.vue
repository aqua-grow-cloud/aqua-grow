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
            <v-text-field
              v-model="form.feedType"
              label="Feed Type"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="4">
            <v-text-field
              v-model.number="form.amount"
              label="Amount (kg)"
              type="number"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
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
import { useAquaStore } from '../stores/useAquaStore';
import { useBatches } from '../composables/useBatches';
import { usePonds } from '../composables/usePonds';
import { feedLogService } from '../services/api';
import type { Batch, FeedLog } from '../api/generated/model';

const store = useAquaStore();

// Use composables
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();
const { data: feedLogs } = feedLogService.useGetAll();

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
  time: '09:00',
  feedType: 'Grower pellets',
  amount: 0
};

const form = ref({ ...defaultForm });

const isValid = computed(() => form.value.batchId && form.value.amount > 0);

const recentLogs = computed(() => {
  return [...(feedLogs.value || [])].sort((a: FeedLog, b: FeedLog) => 
    new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
  ).slice(0, 5);
});

function getBatchLabel(batchId?: string) {
    if (!batchId) return 'Unknown';
    const batch = getBatchById(batchId);
    if (!batch) return batchId;
    const pond = getPondById(batch.pondId);
    return `${pond ? pond.name : 'Unknown'} (${batch.species})`;
}

function save() {
  store.logFeed({ ...form.value });
  // Reset amount but keep other defaults for quick entry
  form.value.amount = 0;
}
</script>

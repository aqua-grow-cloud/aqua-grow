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
          <v-col v-if="form.batchId" cols="12">
            <v-alert variant="tonal" color="info" density="compact">
              <strong>Batch total:</strong> {{ batchTotalKg.toFixed(2) }} kg / {{ batchTotalBags.toFixed(2) }} bags
            </v-alert>
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
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in recentLogs" :key="log.id">
            <td>{{ log.date }}</td>
            <td>{{ getBatchLabel(log.batchId) }}</td>
            <td>{{ log.feedType }}</td>
            <td>{{ log.amount }} kg</td>
            <td>{{ log.time }}</td>
            <td class="text-right">
              <v-btn icon="mdi-pencil" size="x-small" variant="text" color="primary" @click="startEdit(log)"></v-btn>
              <v-btn icon="mdi-delete" size="x-small" variant="text" color="error" @click="confirmDelete(log)"></v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>

  <!-- Edit Dialog -->
  <v-dialog v-model="editDialog" max-width="520px" persistent>
    <v-card>
      <v-card-title>Edit Feed Log</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="saveEdit" ref="editForm">
          <v-row>
            <v-col cols="12" sm="6">
              <v-select
                v-model="editData.batchId"
                :items="activeBatches"
                item-title="label"
                item-value="id"
                label="Batch"
                variant="outlined"
                density="compact"
                required
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="editData.date"
                label="Date"
                type="date"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="4">
              <v-text-field
                v-model="editData.time"
                label="Time"
                type="time"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="4">
              <v-select
                v-model="editData.feedType"
                :items="inventoryItems"
                label="Feed Type"
                variant="outlined"
                density="compact"
                required
              ></v-select>
            </v-col>
            <v-col cols="12" sm="2">
              <v-text-field
                v-model.number="editDisplayAmount"
                :label="`Amount (${editUnit})`"
                type="number"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="2">
              <v-btn-toggle v-model="editUnit" mandatory color="primary" variant="outlined" density="compact">
                <v-btn value="kg">Kg</v-btn>
                <v-btn value="bags">Bags</v-btn>
              </v-btn-toggle>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="editDialog = false">Cancel</v-btn>
        <v-btn color="primary" :loading="updateMutation.isPending.value" @click="saveEdit">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <!-- Delete Confirmation -->
  <v-dialog v-model="deleteDialog" max-width="400px">
    <v-card>
      <v-card-title>Delete Feed Log?</v-card-title>
      <v-card-text>
        Are you sure? The <strong>{{ logToDelete?.amount }} kg</strong> of
        <strong>{{ logToDelete?.feedType }}</strong> logged on
        <strong>{{ logToDelete?.date }}</strong> will be restored to inventory.
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="deleteDialog = false">Cancel</v-btn>
        <v-btn color="error" :loading="deleteMutation.isPending.value" @click="doDelete">Delete</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useQueryClient} from '@tanstack/vue-query';
import {useBatches} from '../../batches/composables/useBatches';
import {usePonds} from '../../ponds/composables/usePonds';
import {feedLogService} from '../api/feedLogService';
import {feedStockService} from '../../inventory/api/feedStockService';
import {fishTypeService} from '../../settings/api/fishTypeService';
import type {Batch, FeedLog} from '@/api/generated/model';
import {useApiAction} from '@/composables/useApiAction.ts';

const queryClient = useQueryClient();

// Use composables
const { batches, getBatchById } = useBatches();
const { getPondById } = usePonds();
const { data: feedLogs } = feedLogService.useGetAll();
const { data: feedStock } = feedStockService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();
const logFeedMutation = feedLogService.useLog();
const updateMutation = feedLogService.useUpdate();
const deleteMutation = feedLogService.useDelete();

const { execute: executeLogFeed, isLoading: isLogging, error: logError } = useApiAction(
    () => logFeedMutation.mutateAsync({ data: form.value } as any),
    { successMessage: 'Feed logged successfully' }
);

const inventoryItems = computed(() => {
  return feedStock.value?.map(s => s.feedType).filter(Boolean) || [];
});

const latestActiveBatchId = computed(() => {
  const active = batches.value?.filter((b: Batch) => b.status === 'Active') || [];
  if (active.length === 0) return '';
  const sorted = active.length === 1
    ? active
    : [...active].sort((a, b) => (b.stockingDate || '').localeCompare(a.stockingDate || ''));
  return sorted.find(() => true)?.id ?? '';
});

const defaultFeedType = computed(() =>
  inventoryItems.value.length === 1 ? inventoryItems.value[0] : ''
);

const unit = ref<'kg' | 'bags'>('kg');
const displayAmount = ref(0);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pondNames = [...new Set(b.fishSeeds?.map(s => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    const speciesList = [...new Set(b.fishSeeds?.map(s => getFishTypeName((s as any).fishTypeId)) || [])].join(', ') || 'Unknown';
    return {
      id: b.id,
      label: b.name
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

// Set batch default once batches load (or immediately if already loaded)
watch(latestActiveBatchId, (id) => {
  if (id && !form.value.batchId) form.value.batchId = id;
}, { immediate: true });

// Auto-select feed type when there is only one option
watch(defaultFeedType, (ft) => {
  if (ft && !form.value.feedType) form.value.feedType = ft;
}, { immediate: true });

const isValid = computed(() => form.value.batchId && displayAmount.value > 0 && form.value.feedType);

const BAGS_TO_KG = 40

const batchTotalKg = computed(() =>
  (feedLogs.value || [])
    .filter(l => l.batchId === form.value.batchId)
    .reduce((sum, l) => sum + (l.amount ?? 0), 0)
)

const batchTotalBags = computed(() => batchTotalKg.value / BAGS_TO_KG)

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

// --- Edit ---
const editDialog = ref(false);
const editData = ref<FeedLog>({});
const editUnit = ref<'kg' | 'bags'>('kg');
const editDisplayAmount = ref(0);

function startEdit(log: FeedLog) {
  editData.value = { ...log };
  editDisplayAmount.value = log.amount ?? 0;
  editUnit.value = 'kg';
  editDialog.value = true;
}

const { execute: executeUpdate, error: updateError } = useApiAction(
    () => updateMutation.mutateAsync({
        id: editData.value.id!,
        data: { ...editData.value, amount: editUnit.value === 'bags' ? editDisplayAmount.value * 40 : editDisplayAmount.value }
    }),
    { successMessage: 'Feed log updated' }
);

async function saveEdit() {
  await executeUpdate();
  if (!updateError.value) {
    editDialog.value = false;
  }
}

// --- Delete ---
const deleteDialog = ref(false);
const logToDelete = ref<FeedLog | null>(null);

function confirmDelete(log: FeedLog) {
  logToDelete.value = log;
  deleteDialog.value = true;
}

const { execute: executeDelete, error: deleteError } = useApiAction(
    () => deleteMutation.mutateAsync(logToDelete.value!.id!),
    { successMessage: 'Feed log deleted' }
);

async function doDelete() {
  await executeDelete();
  if (!deleteError.value) {
    deleteDialog.value = false;
    logToDelete.value = null;
  }
}
</script>

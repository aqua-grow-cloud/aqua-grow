<template>
  <v-container fluid>
    <div class="d-flex align-center mb-4">
       <v-btn icon="mdi-arrow-left" variant="text" @click="$router.back()" class="mr-2"></v-btn>
       <h1 class="text-h5 font-weight-bold">Sample Logs History</h1>
    </div>

    <v-card>
      <v-tabs
        v-model="tab"
        color="primary"
        align-tabs="start"
      >
        <v-tab value="history">Sample History</v-tab>
        <v-tab value="analysis">Growth Analysis</v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <v-window-item value="history">
          <v-card class="mb-4" variant="flat">
      <v-card-text>
        <v-row dense>
          <v-col cols="12" sm="6" md="3">
            <v-select
              v-model="filters.pondId"
              :items="pondOptions"
              item-title="name"
              item-value="id"
              label="Filter by Pond"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
             <v-select
              v-model="filters.batchId"
              :items="batchOptions"
              item-title="name"
              item-value="id"
              label="Filter by Batch"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-select>
          </v-col>
           <v-col cols="12" sm="6" md="3">
             <v-select
              v-model="filters.fishTypeId"
              :items="speciesOptions"
              item-title="name"
              item-value="id"
              label="Filter by Species"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-select>
          </v-col>
           <v-col cols="12" sm="6" md="3">
             <v-text-field
              v-model="filters.startDate"
              type="date"
              label="From Date"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="3">
             <v-text-field
              v-model="filters.endDate"
              type="date"
              label="To Date"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-text-field>
          </v-col>
           <v-col cols="12" sm="6" md="3" class="d-flex align-center">
             <v-btn color="primary" variant="tonal" @click="resetFilters" prepend-icon="mdi-refresh">
               Reset Filters
             </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card variant="flat">
      <v-data-table-server
        v-model:items-per-page="itemsPerPage"
        :headers="headers"
        :items="serverItems"
        :items-length="totalItems"
        :loading="isLoading"
        :sort-by="sortBy"
        item-value="id"
        @update:options="loadItems"
      >
        <template v-slot:item.date="{ item }">
          {{ formatDate(item.date) }}
        </template>

        <template v-slot:item.batchName="{ item }">
           <div class="font-weight-medium">{{ getBatchName(item.batchId) }}</div>
        </template>

        <template v-slot:item.pondName="{ item }">
           {{ getPondName(item.batchId) }}
        </template>

        <template v-slot:item.sampleCount="{ item }">
           {{ item.sampleCount }} pcs
        </template>

        <template v-slot:item.fishTypeId="{ item }">
           {{ getFishTypeName(item.fishTypeId) }}
        </template>

        <template v-slot:item.averageWeight="{ item }">
           {{ (item.averageWeight || 0).toFixed(2) }} g
        </template>

        <template v-slot:item.actions="{ item }">
          <v-btn icon="mdi-pencil" variant="text" size="small" density="compact" color="primary" @click.stop="openEditDialog(item)"></v-btn>
          <v-btn icon="mdi-delete" variant="text" size="small" density="compact" color="error" @click.stop="openDeleteDialog(item)"></v-btn>
        </template>

      </v-data-table-server>
    </v-card>
        </v-window-item>

        <v-window-item value="analysis">
          <v-container fluid>
            <SampleLogPivotTable />
          </v-container>
        </v-window-item>
      </v-window>
    </v-card>
  </v-container>

  <!-- Edit Sample Log Dialog -->
  <SampleLogDialog
    v-model="editDialog"
    :log="editingLog"
    @update="handleUpdate"
  />

  <!-- Delete Confirmation Dialog -->
  <v-dialog v-model="deleteDialog" max-width="400px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-error text-white px-6 py-4">Delete Sample Log</v-card-title>
      <v-card-text class="pt-4">
        Are you sure you want to delete this sample log entry? This action cannot be undone.
      </v-card-text>
      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="deleteDialog = false">Cancel</v-btn>
        <v-btn color="error" variant="flat" @click="confirmDelete">Delete</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {usePonds} from '../ponds/composables/usePonds';
import {useBatches} from '../batches/composables/useBatches';
import {sampleLogService} from './api/sampleLogService';
import {fishTypeService} from '../settings/api/fishTypeService';
import SampleLogPivotTable from './components/SampleLogPivotTable.vue';
import SampleLogDialog from './components/SampleLogDialog.vue';
import type {SampleLog} from '@/api/generated/model';

const router = useRouter();
const { ponds } = usePonds();
const { batches, getBatchById } = useBatches();

const tab = ref('history');

// Edit / delete state
const editDialog = ref(false);
const editingLog = ref<SampleLog | null>(null);
const deleteDialog = ref(false);
const deletingLog = ref<SampleLog | null>(null);

function openEditDialog(log: SampleLog) {
  editingLog.value = log;
  editDialog.value = true;
}

function openDeleteDialog(log: SampleLog) {
  deletingLog.value = log;
  deleteDialog.value = true;
}

async function handleUpdate(payload: SampleLog) {
  if (!payload.id) return;
  try {
    await sampleLogService.update(payload.id, payload);
    editingLog.value = null;
    refetch();
  } catch (e) {
    console.error('Failed to update sample log', e);
  }
}

async function confirmDelete() {
  if (!deletingLog.value?.id) return;
  try {
    await sampleLogService.delete(deletingLog.value.id);
    deleteDialog.value = false;
    deletingLog.value = null;
    refetch();
  } catch (e) {
    console.error('Failed to delete sample log', e);
  }
}

const filters = ref<any>({
  pondId: null,
  batchId: null,
  fishTypeId: null,
  startDate: null,
  endDate: null
});

const itemsPerPage = ref(10);
const page = ref(1);
const sortBy = ref<any[]>([{ key: 'date', order: 'desc' }]);
const totalItems = ref(0);
const serverItems = ref<SampleLog[]>([]);
const isLoading = ref(false);

const headers = [
  { title: 'Date', key: 'date', align: 'start' as const },
  { title: 'Batch', key: 'batchName', align: 'start' as const, sortable: false },
  { title: 'Pond', key: 'pondName', align: 'start' as const, sortable: false },
  { title: 'Species', key: 'fishTypeId', align: 'start' as const },
  { title: 'Sample Count', key: 'sampleCount', align: 'end' as const },
  { title: 'Avg Weight', key: 'averageWeight', align: 'end' as const },
  { title: '', key: 'actions', sortable: false, align: 'end' as const, width: '80px' },
];

const pondOptions = computed(() => ponds.value || []);
const batchOptions = computed(() => batches.value?.map(b => ({ id: b.id, name: b.name })) || []);
const { data: fishTypes } = fishTypeService.useGetAll();
const speciesOptions = computed(() => fishTypes.value || []);

function getFishTypeName(id?: string) {
    if (!id) return '-';
    return (fishTypes.value as any)?.find((t: any) => t.id === id)?.name || id;
}

// Watch filters to reset page and reload
watch(filters, () => {
    page.value = 1;
    loadItems({ page: 1, itemsPerPage: itemsPerPage.value, sortBy: sortBy.value });
}, { deep: true });

async function loadItems({ page: p, itemsPerPage: ipp, sortBy: sort }: any) {
    isLoading.value = true;
    page.value = p;
    itemsPerPage.value = ipp;
    sortBy.value = sort;
}

// Setup the Query
// The query key should depend on filters and pagination
const queryParams = computed(() => ({
    page: page.value - 1, // Spring is 0-indexed
    size: itemsPerPage.value,
    sort: sortBy.value && sortBy.value.length ? `${sortBy.value[0].key},${sortBy.value[0].order}` : 'date,desc',
    ...filters.value
}));

const { data, isFetching, refetch } = sampleLogService.useSearch(queryParams);

// Watch data to update items
watch(data, (newVal) => {
    if (newVal) {
        serverItems.value = newVal.content || [];
        totalItems.value = newVal.totalElements || 0;
    }
}, { immediate: true });

// Sync loading state
watch(isFetching, (val) => {
    isLoading.value = val;
});

function resetFilters() {
    filters.value = {
        pondId: null,
        batchId: null,
        fishTypeId: null,
        startDate: null,
        endDate: null
    };
}

function formatDate(dateStr?: string) {
    if (!dateStr) return '';
    return dateStr;
}

function getBatchName(batchId?: string) {
    if (!batchId) return '-';
    return getBatchById(batchId)?.name || '-';
}

function getPondName(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    if (!batch?.fishSeeds) return '-';

    const pondNames = new Set<string>();
    batch.fishSeeds.forEach((s: any) => {
        if (s.pondId) {
             const p = ponds.value?.find((p: any) => p.id === s.pondId);
             if (p) pondNames.add(p.name || '');
        }
    });
    return Array.from(pondNames).join(', ') || '-';
}
</script>

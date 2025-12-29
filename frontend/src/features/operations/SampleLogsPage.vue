<template>
  <v-container fluid>
    <div class="d-flex align-center mb-4">
       <v-btn icon="mdi-arrow-left" variant="text" @click="$router.back()" class="mr-2"></v-btn>
       <h1 class="text-h5 font-weight-bold">Sample Logs History</h1>
    </div>

    <v-card class="mb-4">
      <v-card-text>
        <v-row dense>
          <v-col cols="12" sm="3">
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
          <v-col cols="12" sm="3">
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
           <v-col cols="12" sm="3">
             <v-select
              v-model="filters.fishType"
              :items="speciesOptions"
              label="Filter by Species"
              variant="outlined"
              density="compact"
              clearable
              hide-details
            ></v-select>
          </v-col>
           <v-col cols="12" sm="3" class="d-flex align-center">
             <v-btn color="primary" variant="tonal" @click="resetFilters" prepend-icon="mdi-refresh">
               Reset Filters
             </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card>
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
        
        <template v-slot:item.averageWeight="{ item }">
           {{ (item.averageWeight || 0).toFixed(2) }} g
        </template>
        
         <template v-slot:item.waterQuality="{ item }">
           <div v-if="item.waterQuality" class="text-caption">
             DO: {{ item.waterQuality.doLevel }} | pH: {{ item.waterQuality.ph }}
           </div>
           <span v-else>-</span>
        </template>

      </v-data-table-server>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { usePonds } from '../ponds/composables/usePonds';
import { useBatches } from '../batches/composables/useBatches';
import { sampleLogService } from './api/sampleLogService';
import type { SampleLog } from '../../api/generated/model';

const router = useRouter();
const { ponds } = usePonds();
const { batches, getBatchById } = useBatches(); 

const filters = ref({
  pondId: null,
  batchId: null,
  fishType: null
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
  { title: 'Species', key: 'fishType', align: 'start' as const },
  { title: 'Sample Count', key: 'sampleCount', align: 'end' as const },
  { title: 'Avg Weight', key: 'averageWeight', align: 'end' as const },
  { title: 'Water Quality', key: 'waterQuality', align: 'start' as const, sortable: false },
];

const pondOptions = computed(() => ponds.value || []);
const batchOptions = computed(() => batches.value?.map(b => ({ id: b.id, name: b.name })) || []);
const speciesOptions = computed(() => {
    // Collect unique species from all batches
    const set = new Set<string>();
    batches.value?.forEach(b => b.fishSeeds?.forEach((s: any) => {
        if (s.type) set.add(s.type);
    }));
    return Array.from(set).sort();
});

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
        fishType: null
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

<template>
  <v-container fluid>
    <v-row>
      <v-col>
        <PageHeader title="Batches">
          <template #actions v-if="store.isManager">
            <v-btn color="primary" variant="elevated" @click="formDialog = true" class="mr-2">
              <v-icon left>mdi-plus</v-icon>
              Start New Batch
            </v-btn>
            <v-btn color="secondary" variant="elevated" @click="seedDialog = true">
              <v-icon left>mdi-sprout</v-icon>
              Add Fish Seed
            </v-btn>
          </template>
        </PageHeader>
      </v-col>
    </v-row>

    <!-- Tabs for Batches vs Fish Seeds -->
    <v-card class="mb-6 elevation-1">
      <v-tabs v-model="activeTab" bg-color="primary" dark>
        <v-tab value="batches">Batches</v-tab>
        <v-tab value="seeds">Fish Seeds</v-tab>
      </v-tabs>

      <v-card-text class="bg-slate-50 border-b py-3 px-6">
        <v-row dense align="center">
          <v-col cols="12" sm="4">
            <v-text-field
              v-model="nameFilter"
              label="Search by Batch Name"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              prepend-inner-icon="mdi-magnify"
              placeholder="Enter batch name..."
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="4">
            <v-select
              v-model="pondFilter"
              :items="pondFilterItems"
              label="Filter by Pond"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              placeholder="All Ponds"
            ></v-select>
          </v-col>
          <v-col cols="12" sm="4" v-if="activeTab === 'seeds'">
            <v-select
              v-model="fishTypeIdFilter"
              :items="fishTypeFilterItems"
              label="Filter by Species"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              placeholder="All Species"
            ></v-select>
          </v-col>
        </v-row>
      </v-card-text>

      <!-- Batches Table -->
      <v-window v-model="activeTab">
        <v-window-item value="batches">
          <v-data-table
            :headers="batchHeaders"
            :items="batches || []"
            :sort-by="[{ key: 'stockingDate', order: 'desc' }]"
            class="elevation-0"
          >
            <template v-slot:item.stockingDate="{ item }">
              {{ item.stockingDate ? new Date(item.stockingDate).toLocaleDateString() : '-' }}
            </template>
            <template v-slot:item.estimatedHarvestDate="{ item }">
              {{ item.estimatedHarvestDate ? new Date(item.estimatedHarvestDate).toLocaleDateString() : '-' }}
            </template>
            <template v-slot:item.status="{ item }">
              <v-chip :color="item.status === 'Active' ? 'success' : 'grey'" size="small">
                {{ item.status }}
              </v-chip>
            </template>
            <template v-slot:item.ponds="{ item }">
              {{ getBatchPonds(item) }}
            </template>
            <template v-slot:item.actions="{ item }">
              <v-btn
                icon="mdi-eye"
                size="small"
                variant="text"
                color="info"
                class="mr-1"
                @click="viewBatchDetails(item)"
                title="View Details"
              ></v-btn>
              <v-btn
                icon="mdi-pencil"
                size="small"
                variant="text"
                color="primary"
                @click="editBatch(item)"
                title="Edit Batch"
              ></v-btn>
            </template>
          </v-data-table>
        </v-window-item>

        <!-- Fish Seeds Table -->
        <v-window-item value="seeds">
          <v-data-table
            :headers="seedHeaders"
            :items="fishSeedList"
            :sort-by="[{ key: 'date', order: 'desc' }]"
            class="elevation-0"
          >
            <template v-slot:item.date="{ item }">
              {{ new Date(item.date).toLocaleDateString() }}
            </template>
            <template v-slot:item.status="{ item }">
              <v-chip
                :color="item.status === 'Active' ? 'success' : 'grey'"
                size="x-small"
                variant="flat"
                class="text-uppercase font-weight-bold"
              >
                {{ item.status }}
              </v-chip>
            </template>
            <template v-slot:item.actions="{ item }" v-if="store.isManager">
              <v-btn
                icon="mdi-truck-delivery"
                size="small"
                variant="text"
                color="secondary"
                @click="openMoveDialog(item)"
                title="Move Fish"
              ></v-btn>
            </template>
          </v-data-table>
        </v-window-item>
      </v-window>
    </v-card>

    <BatchFormDialog
      v-model="formDialog"
      :ponds="ponds || []"
      :batches="batches || []"
      @save="handleBatchFormSave"
      @add-seed="handleAddSeed"
    />

    <BatchManagementDialog
      v-model="batchDialog"
      :batch="editedBatch"
      @save="saveBatch"
    />

    <FishSeedDialog
      v-model="seedDialog"
      :batches="batches || []"
      :ponds="ponds || []"
      :suppliers="suppliers || []"
      @save="handleAddSeed"
    />

    <MoveFishSeedDialog
      v-model="moveDialog"
      :seed="selectedSeed"
      :ponds="ponds || []"
      :loading="isMoving"
      @save="handleMoveSeed"
    />
  </v-container>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useAquaStore} from '@/stores/useAquaStore.ts';
import BatchManagementDialog from './components/BatchManagementDialog.vue';
import BatchFormDialog from './components/BatchFormDialog.vue';
import FishSeedDialog from './components/FishSeedDialog.vue';
import MoveFishSeedDialog from './components/MoveFishSeedDialog.vue';
import PageHeader from '../../components/PageHeader.vue';
import {useBatches} from './composables/useBatches';
import {usePonds} from '../ponds/composables/usePonds';
import {useSuppliers} from '../inventory/composables/useSuppliers';
import {fishTypeService} from '../settings/api/fishTypeService';
import type {Batch as ApiBatch, FishSeed as ApiFishSeed} from '../../api/generated/model';

const router = useRouter();
const store = useAquaStore();
const activeTab = ref('batches');
const batchDialog = ref(false);
const formDialog = ref(false);
const seedDialog = ref(false);
const moveDialog = ref(false);
const editedBatch = ref<ApiBatch | undefined>(undefined);
const selectedSeed = ref<any | null>(null);
const nameFilter = ref('');
const pondFilter = ref<string | null>(null);
const fishTypeIdFilter = ref<string | null>(null);

// Use composables
const { batches, create: createBatch, update: updateBatch, addSeed: addBatchSeed, moveSeed } = useBatches(computed(() => ({
    name: nameFilter.value || undefined,
    pondId: pondFilter.value || undefined,
    fishTypeId: fishTypeIdFilter.value || undefined
})));
const { ponds, getPondById } = usePonds();
const { suppliers } = useSuppliers();
const { data: fishTypes } = fishTypeService.useGetAll();

const batchHeaders = [
  { title: 'Batch Name', key: 'name' },
  { title: 'Stocking Date', key: 'stockingDate' },
  { title: 'Est. Harvest', key: 'estimatedHarvestDate' },
  { title: 'Status', key: 'status' },
  { title: 'Ponds', key: 'ponds' },
  { title: 'Actions', key: 'actions', sortable: false },
];

const seedHeaders = [
  { title: 'Date', key: 'date' },
  { title: 'Batch', key: 'batchName' },
  { title: 'Pond', key: 'pondName' },
  { title: 'Type', key: 'fishTypeName' },
  { title: 'Quantity', key: 'quantity' },
  { title: 'Size (inches)', key: 'sizeInches' },
  { title: 'Size (grams)', key: 'sizeGrams' },
  { title: 'Status', key: 'status' },
  { title: 'Actions', key: 'actions', sortable: false },
];

const fishSeedList = computed(() => {
  const list: any[] = [];
  batches.value?.forEach((batch: ApiBatch) => {
    const batchDisplayName = batch.name || batch.stockingDate || 'Unknown';

    if (batch.fishSeeds && batch.fishSeeds.length > 0) {
        batch.fishSeeds.forEach((seed: ApiFishSeed) => {
            // Client-side filtering to supplement server-side batch filtering
            if (pondFilter.value && seed.pondId !== pondFilter.value) return;
            if (fishTypeIdFilter.value && (seed as any).fishTypeId !== fishTypeIdFilter.value) return;

            const pond = getPondById(seed.pondId || '');
            list.push({
                id: seed.id,
                batchId: batch.id,
                pondId: seed.pondId,
                date: seed.date || '',
                batchName: batchDisplayName,
                pondName: pond ? pond.name : 'Unknown',
                fishTypeName: getFishTypeName((seed as any).fishTypeId),
                quantity: seed.quantity || 0,
                sizeInches: seed.sizeInches || '-',
                sizeGrams: seed.sizeGrams || '-',
                status: seed.status || 'Active'
            });
        });
    } else if (!pondFilter.value && !fishTypeIdFilter.value) {
        // Fallback for batches without seeds - only if no specific filters are active
        list.push({
            id: batch.id,
            date: batch.stockingDate || '',
            batchName: batchDisplayName,
            pondName: 'N/A',
            fishTypeName: 'No Stock',
            quantity: 0,
            sizeInches: '-',
            sizeGrams: '-'
        });
    }
  });
  return list;
});

const pondFilterItems = computed(() => {
    if (!ponds.value) return [];
    return ponds.value.map(p => ({
        title: p.name,
        value: p.id
    })).sort((a, b) => (a.title || '').localeCompare(b.title || ''));
});

const fishTypeFilterItems = computed(() => {
    if (!fishTypes.value) return [];
    return fishTypes.value.map(t => ({
        title: t.name,
        value: t.id
    })).sort((a, b) => a.title.localeCompare(b.title));
});

function getFishTypeName(id?: string) {
    if (!id) return '-';
    return fishTypes.value?.find(t => t.id === id)?.name || id;
}

function getBatchPonds(batch: ApiBatch): string {
  if (!batch.fishSeeds || batch.fishSeeds.length === 0) return 'No ponds';

  const pondIds = [...new Set(batch.fishSeeds.map(s => s.pondId).filter(Boolean))];
  const pondNames = pondIds.map(id => getPondById(id || '')?.name).filter(Boolean);
  const speciesNames = [...new Set(batch.fishSeeds.map(s => getFishTypeName((s as any).fishTypeId)).filter(Boolean))];

  return `${pondNames.join(', ') || 'Unknown'} (${speciesNames.join(', ') || 'Unknown'})`;
}

function viewBatchDetails(batch: ApiBatch) {
  if (batch.id) {
    router.push(`/batches/${batch.id}`);
  }
}

function editBatch(batch: ApiBatch) {
  editedBatch.value = batch;
  batchDialog.value = true;
}

async function saveBatch(batchData: Partial<ApiBatch>) {
  if (batchData.id) {
    await updateBatch(batchData.id, batchData);
  } else {
    await createBatch(batchData);
  }
  batchDialog.value = false;
  editedBatch.value = undefined;
}

async function handleBatchFormSave(batchData: Partial<ApiBatch>) {
    await createBatch(batchData);
    formDialog.value = false;
}

async function handleAddSeed(payload: { batchId: string; seed: ApiFishSeed }) {
    await addBatchSeed(payload.batchId, payload.seed);
    seedDialog.value = false;
}

const isMoving = ref(false);
function openMoveDialog(seed: any) {
    selectedSeed.value = seed;
    moveDialog.value = true;
}

async function handleMoveSeed(payload: any) {
    isMoving.value = true;
    try {
        await moveSeed(payload);
        moveDialog.value = false;
    } finally {
        isMoving.value = false;
    }
}
</script>

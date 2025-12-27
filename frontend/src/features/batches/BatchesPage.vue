<template>
  <v-container fluid>
    <v-row>
      <v-col>
        <PageHeader title="Batches">
          <template #actions v-if="store.isManager">
            <v-btn color="primary" variant="elevated" @click="batchDialog = true" class="mr-2">
              <v-icon left>mdi-plus</v-icon>
              Manage Batches
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
              v-model="speciesFilter"
              :items="speciesFilterItems"
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
                icon="mdi-pencil"
                size="small"
                variant="text"
                color="primary"
                @click="editBatch(item)"
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
          </v-data-table>
        </v-window-item>
      </v-window>
    </v-card>

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
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import BatchManagementDialog from './components/BatchManagementDialog.vue';
import FishSeedDialog from './components/FishSeedDialog.vue';
import PageHeader from '../../components/PageHeader.vue';
import { useBatches } from './composables/useBatches';
import { usePonds } from '../ponds/composables/usePonds';
import { useSuppliers } from '../inventory/composables/useSuppliers';
import type { Batch as ApiBatch, FishSeed as ApiFishSeed } from '../../api/generated/model';

const store = useAquaStore();
const activeTab = ref('batches');
const batchDialog = ref(false);
const seedDialog = ref(false);
const editedBatch = ref<ApiBatch | undefined>(undefined);
const nameFilter = ref('');
const pondFilter = ref<string | null>(null);
const speciesFilter = ref<string | null>(null);

// Use composables
const { batches, create: createBatch, update: updateBatch, addSeed: addBatchSeed } = useBatches(computed(() => ({
    name: nameFilter.value || undefined,
    pondId: pondFilter.value || undefined,
    fishType: speciesFilter.value || undefined
})));
const { ponds, getPondById } = usePonds();
const { suppliers } = useSuppliers();

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
  { title: 'Type', key: 'type' },
  { title: 'Quantity', key: 'quantity' },
  { title: 'Size (inches)', key: 'sizeInches' },
  { title: 'Size (grams)', key: 'sizeGrams' },
];

const fishSeedList = computed(() => {
  const list: any[] = [];
  batches.value?.forEach((batch: ApiBatch) => {
    const batchDisplayName = batch.name || batch.stockingDate || 'Unknown';

    if (batch.fishSeeds && batch.fishSeeds.length > 0) {
        batch.fishSeeds.forEach((seed: ApiFishSeed) => {
            const pond = getPondById(seed.pondId || '');
            list.push({
                id: seed.id,
                date: seed.date || '',
                batchName: batchDisplayName,
                pondName: pond ? pond.name : 'Unknown',
                type: seed.type || 'Unknown',
                quantity: seed.quantity || 0,
                sizeInches: seed.sizeInches || '-',
                sizeGrams: seed.sizeGrams || '-'
            });
        });
    } else {
        // Fallback for batches without seeds
        list.push({
            id: batch.id,
            date: batch.stockingDate || '',
            batchName: batchDisplayName,
            pondName: 'N/A',
            type: 'No Stock',
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

const speciesFilterItems = computed(() => {
    // We can extract all species from all batches or from seeds
    // But since filtering is server-side, it might be better to have an explicit list
    // For now we'll extract from current results or just use known common ones if available
    const species = new Set<string>();
    batches.value?.forEach(b => {
        b.fishSeeds?.forEach(s => {
            if (s.type) species.add(s.type);
        });
    });
    return Array.from(species).sort();
});

function getBatchPonds(batch: ApiBatch): string {
  if (!batch.fishSeeds || batch.fishSeeds.length === 0) return 'No ponds';
  
  const pondIds = [...new Set(batch.fishSeeds.map(s => s.pondId).filter(Boolean))];
  const pondNames = pondIds.map(id => getPondById(id || '')?.name).filter(Boolean);
  
  return pondNames.join(', ') || 'Unknown';
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

async function handleAddSeed(payload: { batchId: string; seed: ApiFishSeed }) {
    await addBatchSeed(payload.batchId, payload.seed);
    seedDialog.value = false;
}
</script>

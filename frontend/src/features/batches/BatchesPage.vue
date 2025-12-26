<template>
  <v-container fluid>
    <PageHeader
      title="Batches"
      :action-label="store.isManager ? 'New Batch / Add Seed' : undefined"
      @action="dialog = true"
    />

    <v-card class="mb-6 elevation-1">
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
          <v-col cols="12" sm="4">
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
      
      <v-data-table
        :headers="headers"
        :items="fishSeedList"
        :sort-by="[{ key: 'date', order: 'desc' }]"
        class="elevation-0"
      >
        <template v-slot:item.date="{ item }">
          {{ new Date(item.date).toLocaleDateString() }}
        </template>
      </v-data-table>
    </v-card>

    <BatchFormDialog 
      v-model="dialog" 
      :ponds="ponds || []" 
      :batches="batches || []"
      @save="saveBatch" 
      @add-seed="handleAddSeed" 
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import BatchFormDialog from './components/BatchFormDialog.vue';
import PageHeader from '../../components/PageHeader.vue';
import { useBatches } from './composables/useBatches';
import { usePonds } from '../ponds/composables/usePonds';
import type { Batch as ApiBatch, FishSeed as ApiFishSeed } from '../../api/generated/model';

const store = useAquaStore();
const dialog = ref(false);
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

const headers = [
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

async function saveBatch(batchData: Omit<ApiBatch, 'id'>) {
  await createBatch(batchData);
  dialog.value = false;
}

async function handleAddSeed(payload: { batchId: string; seed: ApiFishSeed }) {
    await addBatchSeed(payload.batchId, payload.seed);
    dialog.value = false;
}
</script>

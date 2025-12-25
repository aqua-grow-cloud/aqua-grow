<template>
  <v-container fluid>
    <PageHeader
      title="Batches"
      :action-label="store.isManager ? 'Start Batch' : undefined"
      @action="dialog = true"
    />

    <v-card>
      <v-data-table
        :headers="headers"
        :items="fishSeedList"
        :sort-by="[{ key: 'date', order: 'desc' }]"
        class="elevation-0 rounded-lg border"
      >
        <template v-slot:item.date="{ item }">
          {{ new Date(item.date).toLocaleDateString() }}
        </template>
        <!-- Custom formatting if needed -->
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
import { useAquaStore } from '../stores/useAquaStore';
import BatchFormDialog from '../components/BatchFormDialog.vue';
import PageHeader from '../components/PageHeader.vue';
import { useBatches } from '../composables/useBatches';
import { usePonds } from '../composables/usePonds';
import type { Batch as ApiBatch, FishSeed as ApiFishSeed } from '../api/manual/types';

const store = useAquaStore();
const dialog = ref(false);

// Use composables
const { batches, create: createBatch, update: updateBatch } = useBatches();
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
    const pond = getPondById(batch.pondId);// Batch date
    const batchName = batch.stockingDate; // Using Date as Batch Name per example "2024-07-11"

    if (batch.fishSeeds && batch.fishSeeds.length > 0) {
        batch.fishSeeds.forEach((seed: any) => {
            list.push({
                id: seed.id,
                date: seed.date,
                batchName: batchName,
                pondName: pond ? pond.name : 'Unknown',
                type: seed.type,
                quantity: seed.quantity,
                sizeInches: seed.sizeInches || '-',
                sizeGrams: seed.sizeGrams || '-'
            });
        });
    } else {
        // Fallback for batches without explicit seeds (legacy data)
        list.push({
            id: batch.id,
            date: batch.stockingDate,
            batchName: batchName,
            pondName: pond ? pond.name : 'Unknown',
            type: batch.species,
            quantity: batch.initialQuantity,
            sizeInches: '-',
            sizeGrams: batch.seedSize || '-'
        });
    }
  });
  return list;
});

async function saveBatch(batchData: Omit<ApiBatch, 'id'>) {
  await createBatch(batchData);
  dialog.value = false;
}

async function handleAddSeed(payload: { batchId: string; seed: ApiFishSeed }) {
    const batch = batches.value?.find((b: ApiBatch) => b.id === payload.batchId);
    if (!batch) return;

    // Create a copy of existing seeds or empty array
    const updatedSeeds = [...(batch.fishSeeds || []), payload.seed];

    // Optimistically update or just call API
    // We need to send the whole batch object for PUT, usually
    // Assuming backend accepts full object updates
    
    // We construct the updated batch object
    const updatedBatch = {
        ...batch,
        fishSeeds: updatedSeeds
    };

    await updateBatch(batch.id!, updatedBatch);
    dialog.value = false;
}
</script>
